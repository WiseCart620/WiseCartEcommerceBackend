package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.MayaInitiateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.AppSettings;
import com.wisecartecommerce.ecommerce.entity.Cart;
import com.wisecartecommerce.ecommerce.entity.PendingCheckout;
import com.wisecartecommerce.ecommerce.entity.PendingCheckout.PendingCheckoutStatus;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AppSettingsRepository;
import com.wisecartecommerce.ecommerce.repository.CartRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.repository.PendingCheckoutRepository;
import com.wisecartecommerce.ecommerce.util.CouponValidationResult;
import com.wisecartecommerce.ecommerce.util.CouponValidator;

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
    private final CouponValidator couponValidator;
    private final AppSettingsRepository appSettingsRepository;
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("599");

    @Transactional
    public Map<String, String> initiateMayaCheckout(MayaInitiateRequest req) {
        User user = getCurrentUser();

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new CustomException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new CustomException("Cannot checkout with an empty cart");
        }

        BigDecimal subtotal = cart.getSubtotal();

        // ── Re-validate coupon (same logic as OrderServiceImpl) ──────────────
        String couponCode = cart.getCouponCode();
        BigDecimal discount = BigDecimal.ZERO;
        boolean couponFreeShipping = false;

        if (couponCode != null && !couponCode.isBlank()) {
            try {
                CouponValidationResult couponResult = couponValidator.validate(
                        couponCode, subtotal, user.getId(), cart.getItems());
                discount = couponResult.getDiscountAmount();
                couponFreeShipping = couponResult.isFreeShipping();
                log.info("Maya checkout coupon '{}' applied: ₱{} discount", couponCode, discount);
            } catch (CustomException e) {
                log.warn("Coupon '{}' invalid at Maya checkout: {}", couponCode, e.getMessage());
                couponCode = null;
            }
        }

        BigDecimal taxable = subtotal.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal vatRate = getVatRateFromSettings();
        BigDecimal tax = taxable.multiply(vatRate).setScale(2, java.math.RoundingMode.HALF_UP);

        // ── Shipping ──────────────────────────────────────────────────────────
        BigDecimal shipping;
        if (couponFreeShipping) {
            shipping = BigDecimal.ZERO;
        } else if (req.getShippingFee() != null && req.getShippingFee().compareTo(BigDecimal.ZERO) >= 0) {
            shipping = req.getShippingFee();
        } else if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            shipping = BigDecimal.ZERO;
        } else {
            shipping = new BigDecimal("150.00");
        }

        BigDecimal total = taxable.add(shipping).add(tax);

        String checkoutRef = UUID.randomUUID().toString();

        // Generate verification token FIRST
        String verificationToken = generateVerificationToken(checkoutRef);

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
                .verificationToken(verificationToken)
                .build();

        pendingCheckoutRepository.save(pending);

        List<Map<String, Object>> mayaItems = new java.util.ArrayList<>();

        List<com.wisecartecommerce.ecommerce.entity.CartItem> nonAddonItems = cart.getItems().stream()
                .filter(item -> !item.isAddon())
                .collect(java.util.stream.Collectors.toList());

        BigDecimal rawItemsTotal = nonAddonItems.stream()
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal appliedDiscount = discount;

        BigDecimal discountDistributed = BigDecimal.ZERO;

        for (int i = 0; i < nonAddonItems.size(); i++) {
            com.wisecartecommerce.ecommerce.entity.CartItem item = nonAddonItems.get(i);
            BigDecimal itemSubtotal = item.getSubtotal();

            BigDecimal itemDiscount = BigDecimal.ZERO;
            if (appliedDiscount.compareTo(BigDecimal.ZERO) > 0 && rawItemsTotal.compareTo(BigDecimal.ZERO) > 0) {
                if (i == nonAddonItems.size() - 1) {
                    itemDiscount = appliedDiscount.subtract(discountDistributed);
                } else {
                    itemDiscount = appliedDiscount
                            .multiply(itemSubtotal)
                            .divide(rawItemsTotal, 2, java.math.RoundingMode.HALF_UP);
                    discountDistributed = discountDistributed.add(itemDiscount);
                }
            }

            BigDecimal discountedItemTotal = itemSubtotal.subtract(itemDiscount).max(BigDecimal.ZERO);

            String name = item.getProduct().getName()
                    + (item.getVariation() != null ? " - " + item.getVariation().getName() : "");

            mayaItems.add(Map.<String, Object>of(
                    "name", name,
                    "quantity", item.getQuantity(),
                    "totalAmount", Map.of("value", discountedItemTotal, "currency", "PHP")
            ));
        }

        if (shipping.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> shippingItem = new HashMap<>();
            shippingItem.put("name", "Shipping Fee");
            shippingItem.put("totalAmount", Map.of("value", shipping, "currency", "PHP"));
            mayaItems.add(shippingItem);
        }
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> taxItem = new HashMap<>();
            taxItem.put("name", "VAT (" + (vatRate.multiply(new BigDecimal("100")).intValue()) + "%)");
            taxItem.put("totalAmount", Map.of("value", tax, "currency", "PHP"));
            mayaItems.add(taxItem);
        }

        Map<String, String> checkoutResult = mayaService.createCheckoutWithRedirects(
                checkoutRef, total,
                pending.getFirstName(),
                pending.getLastName(),
                pending.getPhone(),
                user.getEmail(),
                null, null, null,
                mayaItems
        );

        String checkoutUrl = checkoutResult.get("redirectUrl");
        String checkoutId = checkoutResult.get("checkoutId");
        pending.setMayaCheckoutUrl(checkoutUrl);
        pending.setMayaCheckoutId(checkoutId);
        pendingCheckoutRepository.save(pending);

        log.info("Maya checkout initiated: ref={} user={} amount={} (subtotal={} discount={} tax={} shipping={})",
                checkoutRef, user.getEmail(), total, subtotal, discount, tax, shipping);

        Map<String, String> result = new HashMap<>();
        result.put("checkoutUrl", checkoutUrl);
        result.put("token", verificationToken);
        return result;
    }

    private String generateVerificationToken(String ref) {
        return UUID.randomUUID().toString();
    }

    private BigDecimal getVatRateFromSettings() {
        try {
            AppSettings settings = appSettingsRepository.findAll().stream()
                    .findFirst()
                    .orElse(null);

            if (settings != null && settings.getVatRate() != null) {
                BigDecimal rate = settings.getVatRate();
                log.debug("Using VAT rate from settings: {}%", rate.multiply(new BigDecimal("100")));
                return rate;
            }

            log.warn("No AppSettings found, using 0% VAT");
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to fetch VAT rate from settings, using 0%", e);
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    public void handlePaymentSuccess(String checkoutRef) {
        PendingCheckout pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef)
                .orElseThrow(() -> new ResourceNotFoundException("Pending checkout not found: " + checkoutRef));

        // ✅ CRITICAL: Check if already completed or failed
        if (pending.getStatus() == PendingCheckoutStatus.COMPLETED) {
            log.info("Already processed for ref={}", checkoutRef);
            return;
        }

        if (pending.getStatus() == PendingCheckoutStatus.FAILED) {
            log.warn("Payment already marked as FAILED for ref={}, cannot process success", checkoutRef);
            throw new CustomException("PAYMENT_FAILED|Payment was previously marked as failed");
        }

        pending.setStatus(PendingCheckoutStatus.PROCESSING);
        pendingCheckoutRepository.saveAndFlush(pending);

        if (pending.getStatus() == PendingCheckoutStatus.EXPIRED
                || LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pending.setStatus(PendingCheckoutStatus.EXPIRED);
            pendingCheckoutRepository.save(pending);
            log.warn("Checkout ref={} has expired", checkoutRef);
            return;
        }

        // ✅ CRITICAL: Verify with Maya BEFORE creating order
        String mayaStatus = mayaService.getCheckoutStatus(pending.getMayaCheckoutId());
        log.info("VERIFYING with Maya: ref={} status={}", checkoutRef, mayaStatus);

        if (!"COMPLETED".equals(mayaStatus) && !"PAYMENT_SUCCESS".equals(mayaStatus)) {
            log.error("❌ REFUSING to create order: Maya payment not completed for ref={}. Status: {}",
                    checkoutRef, mayaStatus);
            pending.setStatus(PendingCheckoutStatus.FAILED);
            pendingCheckoutRepository.save(pending);

            // Check for insufficient balance error code (PY0105)
            if (mayaStatus != null && mayaStatus.contains("PY0105")) {
                throw new CustomException("INSUFFICIENT_BALANCE|PY0105: Account has insufficient balance to perform this transaction. Please add funds to your Maya wallet or use another payment method.");
            }

            if ("EXPIRED".equals(mayaStatus) || (mayaStatus != null && mayaStatus.contains("EXPIRED"))) {
                throw new CustomException("EXPIRED|Payment session expired. Please start a new checkout.");
            } else {
                throw new CustomException("PAYMENT_FAILED|Payment was not successful. Please try again.");
            }
        }

        // Double verify with details endpoint
        Map<String, Object> details = mayaService.getCheckoutDetails(pending.getMayaCheckoutId());
        if (details == null || !"COMPLETED".equals(details.get("status"))) {
            log.error("❌ REFUSING to create order: Maya details don't confirm completion for ref={}", checkoutRef);
            pending.setStatus(PendingCheckoutStatus.FAILED);
            pendingCheckoutRepository.save(pending);
            throw new CustomException("Payment verification failed. Please contact support.");
        }

        log.info("✅ Maya payment verified for ref={}, proceeding with order creation", checkoutRef);

        String mayaPaymentMethod = "Maya";

        // ✅ Use an array to make it effectively final for lambda
        final String[] mayaTransactionReference = {null};

        try {
            Map<String, Object> checkoutDetails = mayaService.getCheckoutDetails(pending.getMayaCheckoutId());
            if (checkoutDetails != null) {
                log.info("Maya checkout details for ref={}: {}", checkoutRef, checkoutDetails);

                // ✅ EXTRACT TRANSACTION REFERENCE NUMBER
                mayaTransactionReference[0] = (String) checkoutDetails.get("transactionReferenceNumber");

                if (mayaTransactionReference[0] == null && checkoutDetails.containsKey("paymentDetails")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> paymentDetails = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                        mayaTransactionReference[0] = (String) paymentDetails.get("transactionReferenceNumber");
                    } catch (Exception ignored) {
                    }
                }

                if (mayaTransactionReference[0] != null) {
                    pending.setMayaTransactionReference(mayaTransactionReference[0]);
                    log.info("✅ Stored transaction reference: {}", mayaTransactionReference[0]);
                }

                // Payment scheme
                if (checkoutDetails.containsKey("paymentScheme")) {
                    mayaPaymentMethod = (String) checkoutDetails.get("paymentScheme");
                }

                // Extract payment ID
                try {
                    String mayaPaymentId = null;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> pd = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responses = (Map<String, Object>) pd.get("responses");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> efs = (Map<String, Object>) responses.get("efs");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> receipt = (Map<String, Object>) efs.get("receipt");
                        mayaPaymentId = (String) receipt.get("transactionId");
                    } catch (Exception e) {
                        mayaPaymentId = (String) checkoutDetails.get("transactionReferenceNumber");
                    }

                    if (mayaPaymentId != null) {
                        pending.setMayaPaymentId(mayaPaymentId);
                        pendingCheckoutRepository.save(pending);
                    }
                } catch (Exception ex) {
                    log.warn("Could not extract Maya paymentId: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch Maya checkout details: {}", e.getMessage());
        }

        pendingCheckoutRepository.save(pending);

        // ── Build and create order ────────────────────────────────────────────
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setPaymentMethod("maya");
        orderRequest.setMayaPaymentMethod(mayaPaymentMethod);
        orderRequest.setNotes(pending.getNotes());
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

        if (pending.getMayaPaymentId() != null) {
            paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                    .ifPresent(payment -> {
                        payment.setMayaPaymentId(pending.getMayaPaymentId());
                        if (mayaTransactionReference[0] != null) {
                            payment.setMayaTransactionReference(mayaTransactionReference[0]);
                        }
                        paymentRepository.save(payment);
                        log.info("Maya paymentId linked to Payment record: orderId={}", order.getId());
                        if (mayaTransactionReference[0] != null) {
                            log.info("Maya transaction reference also stored: {}", mayaTransactionReference[0]);
                        }
                    });
        }

        log.info("Order created from Maya: orderId={} ref={} paymentMethod={}",
                order.getId(), checkoutRef, mayaPaymentMethod);
    }

    @Transactional
    public void handlePaymentFailed(String checkoutRef) {
        pendingCheckoutRepository.findByCheckoutRef(checkoutRef).ifPresent(pending -> {
            pending.setStatus(PendingCheckoutStatus.FAILED);

            // Only fetch from Maya if error message not already set by webhook
            if (pending.getErrorMessage() == null && pending.getMayaCheckoutId() != null) {
                try {
                    String mayaStatus = mayaService.getCheckoutStatus(pending.getMayaCheckoutId());

                    // ✅ Check for cancellation
                    if ("CANCELLED".equals(mayaStatus)) {
                        pending.setErrorMessage("CANCELLED|Payment was cancelled by user");
                        log.info("Payment was CANCELLED for ref={}", checkoutRef);
                    } else if (mayaStatus != null && mayaStatus.contains("|")) {
                        pending.setErrorMessage(mayaStatus); // e.g. "FAILED|PY0119"
                    } else {
                        pending.setErrorMessage("PAYMENT_FAILED|Payment was not successful");
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch Maya status for error details: {}", e.getMessage());
                    pending.setErrorMessage("PAYMENT_FAILED|Payment was not successful");
                }
            } else if (pending.getErrorMessage() == null) {
                pending.setErrorMessage("PAYMENT_FAILED|Payment was not successful");
            }

            pendingCheckoutRepository.save(pending);
            log.info("Maya payment processed for ref={} errorMessage={}", checkoutRef, pending.getErrorMessage());
        });
    }

    @Transactional    

    public String queryAndStoreMayaFailureReason(String checkoutRef, String mayaCheckoutId) {
        Map<String, Object> details = mayaService.getCheckoutDetails(mayaCheckoutId);
        log.info("queryAndStoreMayaFailureReason details for ref={}: {}", checkoutRef, details);

        if (details == null) {
            return null;
        }

        String topLevelStatus = (String) details.get("status");

        if ("CANCELLED".equals(topLevelStatus)) {
            pendingCheckoutRepository.findByCheckoutRef(checkoutRef).ifPresent(pending -> {
                pending.setErrorMessage("CANCELLED|Payment was cancelled by user");
                pending.setStatus(PendingCheckoutStatus.FAILED);
                pendingCheckoutRepository.save(pending);
                log.info("✅ Payment was CANCELLED by user for ref={}", checkoutRef);
            });
            return "CANCELLED";
        }

        String paymentStatus = null;
        String errorCode = null;
        String errorMessage = null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> paymentDetails = (Map<String, Object>) details.get("paymentDetails");
            if (paymentDetails != null) {
                paymentStatus = (String) paymentDetails.get("paymentStatus");

                // ✅ FIX: Look for error in the correct nested structure
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responses = (Map<String, Object>) paymentDetails.get("responses");
                    if (responses != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> efs = (Map<String, Object>) responses.get("efs");
                        if (efs != null) {
                            // ✅ Check for unhandledError array (this is where your error is)
                            List<Map<String, Object>> unhandledErrors = (List<Map<String, Object>>) efs.get("unhandledError");
                            if (unhandledErrors != null && !unhandledErrors.isEmpty()) {
                                Map<String, Object> firstError = unhandledErrors.get(0);
                                Object codeObj = firstError.get("code");
                                if (codeObj != null) {
                                    errorCode = String.valueOf(codeObj);
                                }
                                Object messageObj = firstError.get("message");
                                if (messageObj != null) {
                                    errorMessage = String.valueOf(messageObj);
                                }
                                log.info("✅ Extracted error from unhandledError: code={} message={}", errorCode, errorMessage);
                            }

                            // Fallback: check receipt object
                            if (errorCode == null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> receipt = (Map<String, Object>) efs.get("receipt");
                                if (receipt != null) {
                                    errorCode = (String) receipt.get("responseCode");
                                    errorMessage = (String) receipt.get("responseMessage");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not extract from responses.efs: {}", e.getMessage());
                }

                // Fallback: check paymentDetails directly
                if (errorCode == null) {
                    errorCode = (String) paymentDetails.get("responseCode");
                }
                if (errorCode == null) {
                    errorCode = (String) paymentDetails.get("code");
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract paymentDetails for ref={}: {}", checkoutRef, e.getMessage());
        }

        // Also check top-level for error code
        if (errorCode == null) {
            errorCode = (String) details.get("code");
        }
        if (errorCode == null && details.containsKey("error")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> err = (Map<String, Object>) details.get("error");
                if (err != null) {
                    errorCode = (String) err.get("code");
                }
            } catch (Exception ignored) {
            }
        }

        log.info("Extracted for ref={}: topLevelStatus={} paymentStatus={} errorCode={} errorMessage={}",
                checkoutRef, topLevelStatus, paymentStatus, errorCode, errorMessage);

        // Determine if this is actually a failure
        boolean isActualFailure = "FAILED".equals(paymentStatus)
                || "PAYMENT_FAILED".equals(paymentStatus)
                || errorCode != null;

        if (!isActualFailure && !"FAILED".equals(topLevelStatus)
                && !"EXPIRED".equals(topLevelStatus)
                && !"CANCELLED".equals(topLevelStatus)) {
            log.info("queryAndStoreMayaFailureReason: no failure detected for ref={}", checkoutRef);
            return topLevelStatus;
        }

        final String finalErrorCode = errorCode;
        final String finalErrorMessage = errorMessage;

        pendingCheckoutRepository.findByCheckoutRef(checkoutRef).ifPresent(pending -> {
            if (pending.getStatus() == PendingCheckoutStatus.PENDING
                    || pending.getStatus() == PendingCheckoutStatus.FAILED) {

                String storedError;
                if (finalErrorCode != null) {
                    storedError = finalErrorCode + "|" + resolveErrorDescription(finalErrorCode);
                    if (finalErrorMessage != null) {
                        log.info("Maya error message: {}", finalErrorMessage);
                    }
                } else if ("EXPIRED".equals(topLevelStatus)) {
                    storedError = "EXPIRED|Payment session expired";
                } else if ("CANCELLED".equals(topLevelStatus)) {
                    storedError = "CANCELLED|Payment was cancelled";
                } else {
                    storedError = "PAYMENT_FAILED|Payment was not successful";
                }

                pending.setErrorMessage(storedError);
                pending.setStatus(PendingCheckoutStatus.FAILED);
                pendingCheckoutRepository.save(pending);
                log.info("Stored failure for ref={}: {}", checkoutRef, storedError);
            }
        });

        return errorCode != null ? "FAILED|" + errorCode : topLevelStatus;
    }

    private String resolveErrorDescription(String errorCode) {
        return switch (errorCode) {
            case "2051" ->
                "Insufficient funds";
            case "2041" ->
                "Lost card";
            case "2042" ->
                "Stolen card";
            case "2061" ->
                "Transaction amount limit exceeded";
            case "PY0105" ->
                "Insufficient balance";
            case "PY0123" ->
                "Account limit exceeded";
            case "PY0119" ->
                "Card declined by issuer";
            case "PY0120" ->
                "Card declined";
            case "PY0121" ->
                "Card expired";
            case "PY0002" ->
                "Card expired";
            case "PY0036" ->
                "Card not supported";
            case "PY0127" ->
                "Incomplete customer information";
            case "PY0136" ->
                "Account flagged for security";
            default ->
                "Payment failed with code: " + errorCode;
        };
    }

    @Transactional
    public void handlePaymentSuccessVerified(String checkoutRef, Map<String, Object> verifiedDetails) {
        PendingCheckout pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef)
                .orElseThrow(() -> new ResourceNotFoundException("Pending checkout not found: " + checkoutRef));

        // ✅ Check if already completed or failed
        if (pending.getStatus() == PendingCheckoutStatus.COMPLETED) {
            log.info("Already processed for ref={}", checkoutRef);
            return;
        }

        if (pending.getStatus() == PendingCheckoutStatus.FAILED) {
            log.error("❌ Cannot process success for already failed checkout ref={}", checkoutRef);
            throw new CustomException("PAYMENT_FAILED|Payment was previously marked as failed");
        }

        // ✅ CRITICAL: DO NOT re-query Maya here. We already verified above.
        // Double-check the details we were handed actually confirm COMPLETED.
        if (verifiedDetails == null || !"COMPLETED".equals(verifiedDetails.get("status"))) {
            log.error("❌ handlePaymentSuccessVerified called with non-COMPLETED details for ref={}", checkoutRef);
            pending.setStatus(PendingCheckoutStatus.FAILED);
            pendingCheckoutRepository.save(pending);
            throw new CustomException("PAYMENT_FAILED|Payment details did not confirm completion.");
        }

        // Check if expired
        if (pending.getStatus() == PendingCheckoutStatus.EXPIRED
                || LocalDateTime.now().isAfter(pending.getExpiresAt())) {
            pending.setStatus(PendingCheckoutStatus.EXPIRED);
            pendingCheckoutRepository.save(pending);
            log.warn("Checkout ref={} has expired", checkoutRef);
            throw new CustomException("EXPIRED|Payment session expired. Please start a new checkout.");
        }

        log.info("✅ Maya payment verified for ref={}, proceeding with order creation", checkoutRef);

        String mayaPaymentMethod = "Maya";

        // ✅ Use an array to make it effectively final for lambda
        final String[] mayaTransactionReference = {null};

        try {
            Map<String, Object> checkoutDetails = verifiedDetails; // Use the verified details we already have
            if (checkoutDetails != null) {
                log.info("Maya checkout details for ref={}: {}", checkoutRef, checkoutDetails);

                // ✅ EXTRACT TRANSACTION REFERENCE NUMBER
                mayaTransactionReference[0] = (String) checkoutDetails.get("transactionReferenceNumber");

                if (mayaTransactionReference[0] == null && checkoutDetails.containsKey("paymentDetails")) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> paymentDetails = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                        mayaTransactionReference[0] = (String) paymentDetails.get("transactionReferenceNumber");
                    } catch (Exception ignored) {
                    }
                }

                if (mayaTransactionReference[0] != null) {
                    pending.setMayaTransactionReference(mayaTransactionReference[0]);
                    log.info("✅ Stored transaction reference: {}", mayaTransactionReference[0]);
                }

                // Payment scheme
                if (checkoutDetails.containsKey("paymentScheme")) {
                    mayaPaymentMethod = (String) checkoutDetails.get("paymentScheme");
                }

                // Extract payment ID
                try {
                    String mayaPaymentId = null;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> pd = (Map<String, Object>) checkoutDetails.get("paymentDetails");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responses = (Map<String, Object>) pd.get("responses");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> efs = (Map<String, Object>) responses.get("efs");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> receipt = (Map<String, Object>) efs.get("receipt");
                        mayaPaymentId = (String) receipt.get("transactionId");
                    } catch (Exception e) {
                        mayaPaymentId = (String) checkoutDetails.get("transactionReferenceNumber");
                    }

                    if (mayaPaymentId != null) {
                        pending.setMayaPaymentId(mayaPaymentId);
                        pendingCheckoutRepository.save(pending);
                    }
                } catch (Exception ex) {
                    log.warn("Could not extract Maya paymentId: {}", ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch Maya checkout details: {}", e.getMessage());
        }

        pendingCheckoutRepository.save(pending);

        // ── Build and create order ────────────────────────────────────────────
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setPaymentMethod("maya");
        orderRequest.setMayaPaymentMethod(mayaPaymentMethod);
        orderRequest.setNotes(pending.getNotes());
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

        if (pending.getMayaPaymentId() != null) {
            paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId())
                    .ifPresent(payment -> {
                        payment.setMayaPaymentId(pending.getMayaPaymentId());
                        if (mayaTransactionReference[0] != null) {
                            payment.setMayaTransactionReference(mayaTransactionReference[0]);
                        }
                        paymentRepository.save(payment);
                        log.info("Maya paymentId linked to Payment record: orderId={}", order.getId());
                        if (mayaTransactionReference[0] != null) {
                            log.info("Maya transaction reference also stored: {}", mayaTransactionReference[0]);
                        }
                    });
        }

        log.info("Order created from Maya: orderId={} ref={} paymentMethod={}",
                order.getId(), checkoutRef, mayaPaymentMethod);
    }

    @Transactional
    public Map<String, Object> getCheckoutStatus(String checkoutRef, String token) {
        PendingCheckout pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout not found"));

        if (pending.getVerificationToken() == null || token == null || !pending.getVerificationToken().equals(token)) {
            log.error("Invalid token for checkoutRef: {}", checkoutRef);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "INVALID_TOKEN");
            errorResult.put("isVerified", false);

            if (pending.getVerificationToken() == null && pending.getStatus() == PendingCheckoutStatus.COMPLETED) {
                errorResult.put("message", "This payment has already been processed successfully. Please check your email for order confirmation.");
            } else if (pending.getVerificationToken() == null && pending.getStatus() == PendingCheckoutStatus.FAILED) {
                errorResult.put("message", "This payment has already failed. Please try again with a new checkout.");
            } else if (pending.getVerificationToken() == null) {
                errorResult.put("message", "This verification link has expired or has already been used. Please check your email for order status.");
            } else if (token == null) {
                errorResult.put("message", "Missing verification token. Please use the link from your email or checkout page.");
            } else {
                errorResult.put("message", "Invalid verification token. This link may have expired. Please start a new checkout.");
            }

            return errorResult;
        }

        // ✅ CRITICAL: ALWAYS check with Maya live status - IGNORE database status
        String mayaStatus = null;
        Map<String, Object> mayaDetails = null;

        try {
            mayaStatus = mayaService.getCheckoutStatus(pending.getMayaCheckoutId());
            log.info("🔴 LIVE Maya status for ref={}: mayaId={} status={}",
                    checkoutRef, pending.getMayaCheckoutId(), mayaStatus);

            mayaDetails = mayaService.getCheckoutDetails(pending.getMayaCheckoutId());
            log.info("🔴 Maya details for ref={}: {}", checkoutRef, mayaDetails);
        } catch (Exception e) {
            log.error("Failed to query Maya for ref={}: {}", checkoutRef, e.getMessage());
        }

        // ✅ Determine REAL status from Maya - DO NOT trust database
        boolean isPaymentSuccessful = false;
        String finalStatus = "PENDING";
        String errorMessage = null;

        if (mayaStatus != null) {
            if ("CANCELLED".equals(mayaStatus)) {
                finalStatus = "CANCELLED";
                errorMessage = "CANCELLED|Payment was cancelled by user";
                log.info("ℹ️ Payment CANCELLED by user for ref={}", checkoutRef);
            } else if (mayaStatus.startsWith("FAILED") || mayaStatus.contains("FAILED")
                    || "EXPIRED".equals(mayaStatus)) {
                finalStatus = "FAILED";
                errorMessage = "PAYMENT_FAILED|" + mayaStatus;
                log.error("❌ Payment FAILED in Maya for ref={}: status={}", checkoutRef, mayaStatus);
            } else if (mayaStatus.contains("PY0105") || (mayaDetails != null
                    && String.valueOf(mayaDetails).contains("PY0105"))) {
                finalStatus = "FAILED";
                errorMessage = "PY0105|Insufficient balance in Maya account";
                log.error("❌ Insufficient funds detected for ref={}", checkoutRef);
            } else if ("COMPLETED".equals(mayaStatus) || "PAYMENT_SUCCESS".equals(mayaStatus)) {
                if (mayaDetails != null && "COMPLETED".equals(mayaDetails.get("status"))) {
                    isPaymentSuccessful = true;
                    finalStatus = "COMPLETED";
                    log.info("✅ Payment confirmed in Maya for ref={}", checkoutRef);
                } else {
                    finalStatus = "PENDING";
                    log.info("⏳ Payment pending in Maya for ref={}", checkoutRef);
                }
            }
        }

        if (isPaymentSuccessful && pending.getStatus() != PendingCheckoutStatus.COMPLETED) {
            log.info("Creating order for successful payment: ref={}", checkoutRef);
            handlePaymentSuccessVerified(checkoutRef, mayaDetails);
            pending = pendingCheckoutRepository.findByCheckoutRef(checkoutRef).orElse(pending);
            pending.setVerificationToken(null);
            pendingCheckoutRepository.save(pending);
        } // ✅ Handle FAILURE - NEVER create order
        else if (!isPaymentSuccessful && "FAILED".equals(finalStatus)) {
            if (pending.getStatus() != PendingCheckoutStatus.FAILED) {
                log.warn("Marking checkout as FAILED: ref={} mayaStatus={}", checkoutRef, mayaStatus);
                pending.setStatus(PendingCheckoutStatus.FAILED);
                if (errorMessage != null) {
                    pending.setErrorMessage(errorMessage);
                }
                pending.setVerificationToken(null);
                pendingCheckoutRepository.save(pending);
            }
        }

        // ✅ Build response based on REAL Maya status
        Map<String, Object> result = new HashMap<>();
        result.put("checkoutRef", checkoutRef);

        // ✅ ONLY return COMPLETED if isPaymentSuccessful is TRUE
        if (isPaymentSuccessful && "COMPLETED".equals(finalStatus)) {
            result.put("status", "COMPLETED");
            result.put("isVerified", true);
            if (pending.getOrderId() != null) {
                result.put("orderId", pending.getOrderId());
                try {
                    OrderResponse orderResponse = orderService.getOrderById(pending.getOrderId());
                    if (orderResponse != null) {
                        if (orderResponse.getOrderNumber() != null) {
                            result.put("orderNumber", orderResponse.getOrderNumber());
                        }
                        if (orderResponse.getTrackingNumber() != null) {
                            result.put("trackingNumber", orderResponse.getTrackingNumber());
                            result.put("flashTrackingNumber", orderResponse.getTrackingNumber());
                        }
                        if (pending.getUser() != null && pending.getUser().getEmail() != null) {
                            result.put("email", pending.getUser().getEmail());
                        } else if (pending.getGuestEmail() != null) {
                            result.put("email", pending.getGuestEmail());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch order details: {}", e.getMessage());
                }
            }
        } else {
            result.put("status", finalStatus);
            result.put("isVerified", false);
            if (errorMessage != null) {
                result.put("message", errorMessage);
            } else if (pending.getErrorMessage() != null) {
                result.put("message", pending.getErrorMessage());
            }
        }

        log.info("Returning status for ref={}: status={} isVerified={}", checkoutRef, result.get("status"), result.get("isVerified"));
        return result;
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
