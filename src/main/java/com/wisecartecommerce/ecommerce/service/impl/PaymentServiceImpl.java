package com.wisecartecommerce.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    
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
        
        // Validate payment amount
        if (request.getAmount() == null) {
            request.setAmount(order.getFinalAmount());
        }
        
        if (request.getAmount().compareTo(order.getFinalAmount()) != 0) {
            throw new CustomException("Payment amount must match order total: " + order.getFinalAmount());
        }
        
        // Process payment based on payment method
        PaymentStatus paymentStatus;
        String transactionId = generateTransactionId();
        
        try {
            // Simulate payment processing
            // In real implementation, integrate with payment gateway (Stripe, PayPal, etc.)
            paymentStatus = processPaymentWithGateway(request, transactionId);
            
        } catch (Exception e) {
            log.error("Payment processing failed for order {}: {}", order.getOrderNumber(), e.getMessage());
            paymentStatus = PaymentStatus.FAILED;
        }
        
        // Create payment record
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
            payment.setCardLastFour(request.getCardNumber() != null ? 
                    request.getCardNumber().substring(request.getCardNumber().length() - 4) : null);
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
        
        // Send payment confirmation email
        if (paymentStatus == PaymentStatus.COMPLETED) {
            emailService.sendOrderConfirmationEmail(order);
        }
        
        log.info("Payment processed for order {}: {} - {}", 
                order.getOrderNumber(), paymentStatus, transactionId);
        
        return mapToPaymentResponse(savedPayment);
    }
    
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
        
        Payment payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED)
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
        
        if (payment.getOrder().getDeliveredAt() == null || 
            payment.getOrder().getDeliveredAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new CustomException("Refund period (30 days) has expired");
        }
        
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(payment.getAmount());
        payment.setRefundReason(reason);
        payment.setRefundedAt(LocalDateTime.now());
        
        // Update order status
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
            "BANK_TRANSFER"
        );
    }
    
    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        // Handle payment gateway webhook
        // This would typically verify the signature and process the webhook event
        log.info("Received payment webhook: {}", payload);
        
        // Parse payload and update payment status based on gateway response
        // For example, Stripe webhook for payment_intent.succeeded, payment_intent.payment_failed, etc.
    }
    
    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        
        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(status);
        
        switch (status) {
            case COMPLETED:
                payment.setCompletedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.COMPLETED);
                payment.getOrder().setStatus(OrderStatus.PROCESSING);
                break;
            case FAILED:
                payment.setFailedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.FAILED);
                break;
            case REFUNDED:
                payment.setRefundedAt(LocalDateTime.now());
                payment.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
                payment.getOrder().setStatus(OrderStatus.REFUNDED);
                break;
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
    
    private PaymentStatus processPaymentWithGateway(PaymentRequest request, String transactionId) {
        // Simulate payment processing
        // In real implementation, this would call the actual payment gateway API
        
        log.info("Processing {} payment of {} for transaction {}", 
                request.getPaymentMethod(), request.getAmount(), transactionId);
        
        // Simulate different scenarios
        if (request.getPaymentMethod().equals("COD")) {
            return PaymentStatus.PENDING; // Cash on delivery is pending until delivered
        }
        
        if (request.getCardNumber() != null && request.getCardNumber().endsWith("9999")) {
            // Simulate failed payment for specific card
            throw new RuntimeException("Payment declined by bank");
        }
        
        // Simulate successful payment
        return PaymentStatus.COMPLETED;
    }
    
    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
    
    private String getPaymentGateway(String paymentMethod) {
        switch (paymentMethod) {
            case "CREDIT_CARD":
            case "DEBIT_CARD":
                return "Stripe";
            case "PAYPAL":
                return "PayPal";
            case "COD":
                return "CashOnDelivery";
            case "BANK_TRANSFER":
                return "BankTransfer";
            default:
                return "Unknown";
        }
    }
    
    private String detectCardBrand(String cardNumber) {
        if (cardNumber == null) return null;
        
        if (cardNumber.startsWith("4")) {
            return "Visa";
        } else if (cardNumber.startsWith("5")) {
            return "MasterCard";
        } else if (cardNumber.startsWith("3")) {
            return "American Express";
        } else if (cardNumber.startsWith("6")) {
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