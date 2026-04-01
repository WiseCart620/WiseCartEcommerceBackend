package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.MayaInitiateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.Cart;
import com.wisecartecommerce.ecommerce.entity.PendingCheckout;
import com.wisecartecommerce.ecommerce.entity.PendingCheckout.PendingCheckoutStatus;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.CartRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.repository.PendingCheckoutRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MayaCheckoutService {

    private final MayaService mayaService;
    private final PendingCheckoutRepository pendingCheckoutRepository;
    private final CartRepository cartRepository;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;

    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("599");

    @Transactional
    public String initiateMayaCheckout(MayaInitiateRequest req) {
        User user = getCurrentUser();

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new CustomException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new CustomException("Cannot checkout with an empty cart");
        }

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal tax = taxable.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal shipping;
        if (req.getShippingFee() != null && req.getShippingFee().compareTo(BigDecimal.ZERO) >= 0) {
            shipping = req.getShippingFee();
        } else if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            shipping = BigDecimal.ZERO;
        } else {
            shipping = new BigDecimal("150.00");
        }

        BigDecimal total = taxable.add(shipping).add(tax);

        String checkoutRef = "WC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        PendingCheckout pending = PendingCheckout.builder()
                .checkoutRef(checkoutRef)
                .user(user)
                .status(PendingCheckoutStatus.PENDING)
                .paymentMethod("maya")
                .shippingAddressId(req.getShippingAddressId())
                .addressLine1(req.getAddressLine1())
                .addressLine2(req.getAddressLine2())
                .city(req.getCity())
                .state(req.getState())
                .postalCode(req.getPostalCode())
                .country(req.getCountry())
                .phone(req.getPhone())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .shippingFee(shipping)
                .expressCategory(req.getExpressCategory())
                .couponCode(cart.getCouponCode())
                .notes(req.getNotes())
                .amount(total)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        pendingCheckoutRepository.save(pending);

        Map<String, String> checkoutResult = mayaService.createCheckout(checkoutRef, total);
        String checkoutUrl = checkoutResult.get("redirectUrl");
        String checkoutId = checkoutResult.get("checkoutId");
        pending.setMayaCheckoutUrl(checkoutUrl);
        pending.setMayaCheckoutId(checkoutId);
        pendingCheckoutRepository.save(pending);

        log.info("Maya checkout initiated: ref={} user={} amount={}", checkoutRef, user.getEmail(), total);
        return checkoutUrl;
    }

    @Transactional
    public void handlePaymentSuccess(String checkoutRef) {
        PendingCheckout pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef)
                .orElseThrow(() -> new ResourceNotFoundException("Pending checkout not found: " + checkoutRef));

        if (pending.getStatus() == PendingCheckoutStatus.COMPLETED) {
            log.info("Webhook already processed for ref={}", checkoutRef);
            return;
        }

        if (pending.getStatus() == PendingCheckoutStatus.EXPIRED
                || LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pending.setStatus(PendingCheckoutStatus.EXPIRED);
            pendingCheckoutRepository.save(pending);
            log.warn("Checkout ref={} has expired", checkoutRef);
            return;
        }

        // ── Resolve payment method & extract paymentId ───────────────────────
        String mayaPaymentMethod = "Maya";
        try {
            Map<String, Object> checkoutDetails = mayaService.getCheckoutDetails(pending.getMayaCheckoutId());
            if (checkoutDetails != null) {
                log.warn("=== MAYA CHECKOUT DETAILS for ref={} ===: {}", checkoutRef, checkoutDetails);
                log.warn("Response keys: {}", checkoutDetails.keySet());

                // Payment scheme at root level
                if (checkoutDetails.containsKey("paymentScheme")) {
                    mayaPaymentMethod = (String) checkoutDetails.get("paymentScheme");
                    log.warn("Found paymentScheme: {}", mayaPaymentMethod);
                }

                // Dig into card number for specific brand
                if (checkoutDetails.containsKey("paymentDetails")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> paymentDetails = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                        log.warn("Payment details keys: {}", paymentDetails.keySet());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responses = (Map<String, Object>) paymentDetails.get("responses");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> efs = (Map<String, Object>) responses.get("efs");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> payer = (Map<String, Object>) efs.get("payer");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fundingInstrument = (Map<String, Object>) payer.get("fundingInstrument");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> card = (Map<String, Object>) fundingInstrument.get("card");
                        String cardNumber = (String) card.get("cardNumber");
                        if (cardNumber != null) {
                            if (cardNumber.startsWith("4")) {
                                mayaPaymentMethod = "VISA";
                            } else if (cardNumber.startsWith("5")) {
                                mayaPaymentMethod = "Mastercard";
                            } else if (cardNumber.startsWith("3")) {
                                mayaPaymentMethod = "JCB";
                            }
                            log.warn("Detected card from number: {}", mayaPaymentMethod);
                        }
                    } catch (Exception ignored) {
                    }
                }

                log.info("Maya payment method resolved for ref={}: {}", checkoutRef, mayaPaymentMethod);

                // ── Extract Maya paymentId for refunds ────────────────────────
                // Primary: transactionReferenceNumber at root level
                // Your logs confirm this field is present: transactionReferenceNumber=e8525301-...
                try {
                    String mayaPaymentId = (String) checkoutDetails.get("transactionReferenceNumber");

                    // Fallback: paymentDetails.responses.efs.paymentTransactionReferenceNo
                    if (mayaPaymentId == null && checkoutDetails.containsKey("paymentDetails")) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> pd = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responses = (Map<String, Object>) pd.get("responses");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> efs = (Map<String, Object>) responses.get("efs");
                            mayaPaymentId = (String) efs.get("paymentTransactionReferenceNo");
                        } catch (Exception ignored) {
                        }
                    }

// In handlePaymentSuccess(), after extracting paymentId
                    if (mayaPaymentId != null) {
                        pending.setMayaPaymentId(mayaPaymentId);
                        pendingCheckoutRepository.save(pending);
                        log.info("Maya paymentId saved: {} for ref={}", mayaPaymentId, checkoutRef);

                        // Also log the checkoutId for reference
                        log.info("Maya checkoutId: {} for ref={}", pending.getMayaCheckoutId(), checkoutRef);
                    } else {
                        log.warn("Maya paymentId not found in response for ref={}", checkoutRef);
                        // Log full response to see what's available
                        Map<String, Object> details = mayaService.getCheckoutDetails(pending.getMayaCheckoutId());
                        log.warn("Full Maya response keys: {}", details != null ? details.keySet() : "null");
                    }
                } catch (Exception ex) {
                    log.warn("Could not extract Maya paymentId for ref={}: {}", checkoutRef, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch Maya checkout details for ref={}: {}", checkoutRef, e.getMessage(), e);
        }

        // ── Build and create order ────────────────────────────────────────────
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setPaymentMethod("maya");
        orderRequest.setMayaPaymentMethod(mayaPaymentMethod);
        orderRequest.setNotes(pending.getNotes());
        orderRequest.setShippingFee(pending.getShippingFee());
        orderRequest.setExpressCategory(pending.getExpressCategory());

        if (pending.getShippingAddressId() != null) {
            orderRequest.setShippingAddressId(pending.getShippingAddressId());
        } else {
            OrderRequest.AddressData addr = new OrderRequest.AddressData();
            addr.setFirstName(pending.getFirstName());
            addr.setLastName(pending.getLastName());
            addr.setAddressLine1(pending.getAddressLine1());
            addr.setAddressLine2(pending.getAddressLine2());
            addr.setCity(pending.getCity());
            addr.setState(pending.getState());
            addr.setPostalCode(pending.getPostalCode());
            addr.setCountry(pending.getCountry());
            addr.setPhone(pending.getPhone());
            orderRequest.setShippingAddress(addr);
        }

        OrderResponse order = orderService.createOrderForUser(pending.getUser(), orderRequest);

        pending.setOrderId(order.getId());
        pending.setStatus(PendingCheckoutStatus.COMPLETED);
        pendingCheckoutRepository.save(pending);

        // ── Link paymentId to Payment record so refunds work ─────────────────
        if (pending.getMayaPaymentId() != null) {
            paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                    .ifPresent(payment -> {
                        payment.setMayaPaymentId(pending.getMayaPaymentId());
                        paymentRepository.save(payment);
                        log.info("Maya paymentId linked to Payment record: orderId={} paymentId={}",
                                order.getId(), pending.getMayaPaymentId());
                    });
        } else {
            log.warn("Maya paymentId is null — refund will not be possible for orderId={}", order.getId());
        }

        log.info("Order created from Maya webhook: orderId={} ref={} paymentMethod={}",
                order.getId(), checkoutRef, mayaPaymentMethod);
    }

    @Transactional
    public void handlePaymentFailed(String checkoutRef) {
        pendingCheckoutRepository.findByCheckoutRef(checkoutRef).ifPresent(pending -> {
            pending.setStatus(PendingCheckoutStatus.FAILED);
            pendingCheckoutRepository.save(pending);
            log.info("Maya payment failed for ref={}", checkoutRef);
        });
    }

    @Transactional
    public Map<String, Object> getCheckoutStatus(String checkoutRef) {
        PendingCheckout pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout not found"));

        if (pending.getStatus() == PendingCheckoutStatus.PENDING) {
            try {
                String mayaStatus = mayaService.getCheckoutStatus(pending.getMayaCheckoutId());
                log.info("Maya live status for ref={}: mayaId={} status={}",
                        checkoutRef, pending.getMayaCheckoutId(), mayaStatus);

                if ("COMPLETED".equals(mayaStatus)) {
                    handlePaymentSuccess(checkoutRef);
                    pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef).orElseThrow();
                } else if ("FAILED".equals(mayaStatus)
                        || "EXPIRED".equals(mayaStatus)
                        || "CANCELLED".equals(mayaStatus)) {
                    handlePaymentFailed(checkoutRef);
                    pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef).orElseThrow();
                }
            } catch (Exception e) {
                log.warn("Could not fetch Maya status for ref={}: {}", checkoutRef, e.getMessage());
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", pending.getStatus().name());
        result.put("checkoutRef", checkoutRef);
        if (pending.getOrderId() != null) {
            result.put("orderId", pending.getOrderId());
        }
        return result;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
