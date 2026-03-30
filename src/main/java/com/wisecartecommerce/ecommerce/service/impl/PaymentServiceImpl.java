package com.wisecartecommerce.ecommerce.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.PaymentRequest;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.Payment;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.service.EmailService;
import com.wisecartecommerce.ecommerce.service.PaymentResponse;
import com.wisecartecommerce.ecommerce.service.PaymentService;
import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.util.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    // ══════════════════════════════════════════════════════════════════════════
    // Process payment
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only pay for your own orders");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException("Order cannot be paid in current status: " + order.getStatus());
        }

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new CustomException("Order has already been paid");
        }

        // ── COD — no payment processing needed ───────────────────────────────
        // Flash Express will collect the cash on delivery.
        // We confirm the order immediately so admin can pack and ship it.
        boolean isCod = isCod(request.getPaymentMethod());
        if (isCod) {
            return processCodPayment(order, user);
        }

        // ── Online payment ────────────────────────────────────────────────────
        if (request.getAmount() == null) {
            request.setAmount(order.getFinalAmount());
        }

        if (request.getAmount().compareTo(order.getFinalAmount()) != 0) {
            throw new CustomException("Payment amount must match order total: " + order.getFinalAmount());
        }

        PaymentStatus paymentStatus;
        String transactionId = generateTransactionId();

        try {
            paymentStatus = processPaymentWithGateway(request, transactionId);
        } catch (Exception e) {
            log.error("Payment processing failed for order {}: {}", order.getOrderNumber(), e.getMessage());
            paymentStatus = PaymentStatus.FAILED;
        }

        Payment payment = Payment.builder()
                .order(order)
                .transactionId(transactionId)
                .amount(request.getAmount())
                .status(paymentStatus)
                .paymentMethod(request.getPaymentMethod())
                .paymentGateway(getPaymentGateway(request.getPaymentMethod()))
                .payerEmail(user.getEmail())
                .payerName(user.getFirstName() + " " + user.getLastName())
                .build();

        if (request.getPaymentMethod().equals("CREDIT_CARD") || request.getPaymentMethod().equals("DEBIT_CARD")) {
            payment.setCardLastFour(request.getCardNumber() != null
                    ? request.getCardNumber().substring(request.getCardNumber().length() - 4) : null);
            payment.setCardBrand(detectCardBrand(request.getCardNumber()));
        } else if (request.getPaymentMethod().equals("PAYPAL")) {
            payment.setPayerEmail(request.getPaypalEmail());
        } else if (request.getPaymentMethod().equals("BANK_TRANSFER")) {
            payment.setPayerName(request.getBankAccount());
        }

        if (paymentStatus == PaymentStatus.COMPLETED) {
            payment.setCompletedAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setStatus(OrderStatus.PROCESSING);
        } else if (paymentStatus == PaymentStatus.FAILED) {
            payment.setFailedAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.FAILED);
        }

        Payment savedPayment = paymentRepository.save(payment);
        order.getPayments().add(savedPayment);
        orderRepository.save(order);

        if (paymentStatus == PaymentStatus.COMPLETED) {
            emailService.sendOrderConfirmationEmail(order);
        }

        log.info("Payment processed for order {}: {} - {}",
                order.getOrderNumber(), paymentStatus, transactionId);

        return mapToPaymentResponse(savedPayment);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COD payment handler
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * COD orders don't require upfront payment. Flash Express will collect cash
     * when the courier delivers the parcel. We create a PENDING payment record
     * and move the order to PROCESSING so admin can pack and ship it right
     * away. Payment status is set to COMPLETED by the Flash webhook on delivery
     * (state 5).
     */
    private PaymentResponse processCodPayment(Order order, User user) {
        String transactionId = generateTransactionId();

        Payment payment = Payment.builder()
                .order(order)
                .transactionId(transactionId)
                .amount(order.getFinalAmount())
                .status(PaymentStatus.PENDING) // cash not collected yet
                .paymentMethod("COD")
                .paymentGateway("CashOnDelivery")
                .payerEmail(user.getEmail())
                .payerName(user.getFirstName() + " " + user.getLastName())
                .gatewayResponse("COD confirmed — Flash Express will collect on delivery")
                .build();

        // Confirm the order so admin can pack + ship it
        order.setPaymentStatus(PaymentStatus.PENDING);  // still pending until delivered
        order.setStatus(OrderStatus.PROCESSING);        // but confirmed — pack it!

        Payment savedPayment = paymentRepository.save(payment);
        order.getPayments().add(savedPayment);
        orderRepository.save(order);

        emailService.sendOrderConfirmationEmail(order);

        log.info("COD order confirmed: {} | ₱{} to be collected on delivery",
                order.getOrderNumber(), order.getFinalAmount());

        return mapToPaymentResponse(savedPayment);
    }

    /**
     * Called by FlashWebhookController when webhook state = 5 (Delivered).
     * Marks the COD payment as COMPLETED because Flash has collected the cash.
     */
    @Transactional
    public void completeCodPaymentOnDelivery(Order order) {
        paymentRepository.findPendingCodByOrderId(order.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setGatewayResponse("Cash collected by Flash Express courier on delivery");
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.COMPLETED);
            orderRepository.save(order);

            log.info("COD payment completed for order {}: ₱{} collected by courier",
                    order.getOrderNumber(), payment.getAmount());
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Read operations
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        User user = getCurrentUser();
        if (!payment.getOrder().getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new CustomException("You can only view your own payments");
        }

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = getCurrentUser();
        if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new CustomException("You can only view your own payments");
        }

        // For COD orders return the pending payment record
        Payment payment = isCod(order.getPaymentMethod())
                ? paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("No payment record found for order"))
                : paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED)
                        .orElseThrow(() -> new ResourceNotFoundException("No completed payment found for order"));

        return mapToPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse requestRefund(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        User user = getCurrentUser();
        if (!payment.getOrder().getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only request refund for your own payments");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new CustomException("Only completed payments can be refunded");
        }

        // COD refunds: customer has already paid cash to courier.
        // Flash Express handles the physical cash return separately.
        if (isCod(payment.getPaymentMethod())) {
            log.info("COD refund requested for order {} — manual cash return required",
                    payment.getOrder().getOrderNumber());
        }

        if (payment.getOrder().getDeliveredAt() == null
                || payment.getOrder().getDeliveredAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new CustomException("Refund period (30 days) has expired");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundReason(reason);
        payment.setRefundedAt(LocalDateTime.now());

        payment.getOrder().setStatus(OrderStatus.REFUNDED);
        payment.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);

        Payment refundedPayment = paymentRepository.save(payment);
        log.info("Refund requested for payment {}: {}", paymentId, reason);

        return mapToPaymentResponse(refundedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory() {
        User user = getCurrentUser();
        return paymentRepository.findByUserId(user.getId()).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAvailablePaymentMethods() {
        return Arrays.asList(
                "CREDIT_CARD",
                "DEBIT_CARD",
                "PAYPAL",
                "COD",
                "BANK_TRANSFER",
                "MAYA"
        );
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        log.info("Received payment webhook: {}", payload);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(status);

        switch (status) {
            case COMPLETED -> {
                payment.setCompletedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.COMPLETED);
                payment.getOrder().setStatus(OrderStatus.PROCESSING);
            }
            case FAILED -> {
                payment.setFailedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.FAILED);
            }
            case REFUNDED -> {
                payment.setRefundedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
                payment.getOrder().setStatus(OrderStatus.REFUNDED);
            }
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Payment {} status updated from {} to {}", paymentId, oldStatus, status);
        return mapToPaymentResponse(updatedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToPaymentResponse(payment);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════
    private boolean isCod(String paymentMethod) {
        return "COD".equalsIgnoreCase(paymentMethod)
                || "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod);
    }

    private PaymentStatus processPaymentWithGateway(PaymentRequest request, String transactionId) {
        log.info("Processing {} payment of {} for transaction {}",
                request.getPaymentMethod(), request.getAmount(), transactionId);

        if (request.getCardNumber() != null && request.getCardNumber().endsWith("9999")) {
            throw new RuntimeException("Payment declined by bank");
        }

        return PaymentStatus.COMPLETED;
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String getPaymentGateway(String paymentMethod) {
        return switch (paymentMethod.toUpperCase()) {
            case "CREDIT_CARD", "DEBIT_CARD" ->
                "Stripe";
            case "PAYPAL" ->
                "PayPal";
            case "COD", "CASH_ON_DELIVERY" ->
                "CashOnDelivery";
            case "BANK_TRANSFER" ->
                "BankTransfer";
            case "MAYA" ->
                "Maya";
            default ->
                "Unknown";
        };
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        if (cardNumber.startsWith("4")) {
            return "Visa";
        }
        if (cardNumber.startsWith("5")) {
            return "MasterCard";
        }
        if (cardNumber.startsWith("3")) {
            return "American Express";
        }
        if (cardNumber.startsWith("6")) {
            return "Discover";
        }
        return "Unknown";
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentGateway(payment.getPaymentGateway())
                .cardLastFour(payment.getCardLastFour())
                .cardBrand(payment.getCardBrand())
                .payerEmail(payment.getPayerEmail())
                .payerName(payment.getPayerName())
                .refundAmount(payment.getRefundAmount())
                .refundReason(payment.getRefundReason())
                .refundedAt(payment.getRefundedAt())
                .gatewayResponse(payment.getGatewayResponse())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .failedAt(payment.getFailedAt())
                .build();
    }

}
