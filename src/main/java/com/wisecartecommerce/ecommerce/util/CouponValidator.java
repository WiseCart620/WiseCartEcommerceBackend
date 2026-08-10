package com.wisecartecommerce.ecommerce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponValidator {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderRepository orderRepository;

    /**
     * Validates a coupon code against the given subtotal and cart items. Pass
     * userId = null for guest orders (skips per-user usage check). Pass
     * cartItems = null to skip the minimum product quantity check.
     *
     * @throws CustomException with a user-friendly message on any failure
     */
    /**
     * Validates and prices a set of coupon codes applied together, enforcing
     * combinability rules and preventing the combined discount from exceeding
     * the subtotal.
     */
    public java.util.List<CouponValidationResult> validateCombination(
            java.util.List<String> couponCodes, BigDecimal subtotal,
            Long userId, List<CartItem> cartItems) {

        java.util.List<CouponValidationResult> results = new java.util.ArrayList<>();
        for (String code : couponCodes) {
            results.add(validate(code, subtotal, userId, cartItems));
        }

        if (results.size() > 1) {
            for (CouponValidationResult r : results) {
                Coupon c = r.getCoupon();
                if (!Boolean.TRUE.equals(c.getIsCombinable())) {
                    throw new CustomException(
                            "Coupon '" + c.getCode() + "' cannot be combined with other coupons");
                }
                Set<Long> restrictedTo = c.getCombinableWith();
                if (restrictedTo != null && !restrictedTo.isEmpty()) {
                    for (CouponValidationResult other : results) {
                        if (other == r) {
                            continue;
                        }
                        if (!restrictedTo.contains(other.getCoupon().getId())) {
                            throw new CustomException(
                                    "Coupon '" + c.getCode() + "' cannot be combined with '"
                                    + other.getCoupon().getCode() + "'");
                        }
                    }
                }
            }
        }

        // Cap combined percentage/fixed discount so total never exceeds subtotal
        BigDecimal totalDiscount = results.stream()
                .map(CouponValidationResult::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDiscount.compareTo(subtotal) > 0) {
            BigDecimal excess = totalDiscount.subtract(subtotal);
            // trim the excess off the last discountable (non-free-shipping) coupon
            for (int i = results.size() - 1; i >= 0 && excess.compareTo(BigDecimal.ZERO) > 0; i--) {
                CouponValidationResult r = results.get(i);
                if (r.isFreeShipping()) {
                    continue;
                }
                BigDecimal reduceBy = r.getDiscountAmount().min(excess);
                r.setDiscountAmount(r.getDiscountAmount().subtract(reduceBy));
                excess = excess.subtract(reduceBy);
            }
        }

        return results;
    }

    public CouponValidationResult validate(String couponCode, BigDecimal subtotal,
            Long userId, List<CartItem> cartItems) {
        return validate(couponCode, subtotal, userId, cartItems, null);
    }

    public CouponValidationResult validate(String couponCode, BigDecimal subtotal,
            Long userId, List<CartItem> cartItems, String guestEmail) {
        return validate(couponCode, subtotal, userId, cartItems, guestEmail, null);
    }

    public CouponValidationResult validate(String couponCode, BigDecimal subtotal,
            Long userId, List<CartItem> cartItems, String guestEmail, String guestIp) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new CustomException("Coupon code is required");
        }

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode.toUpperCase())
                .orElseThrow(() -> new CustomException(
                "Coupon code '" + couponCode + "' is invalid or inactive"));

        // ── Guest-specific checks ────────────────────────────────────────────
        if (userId == null) {
            if (!Boolean.TRUE.equals(coupon.getAllowGuestCheckout())) {
                throw new CustomException(
                        "Coupon '" + couponCode + "' is not available for guest checkout. Please sign in to use it.");
            }
            if (coupon.getMaxUsagePerUser() != null && guestEmail != null && !guestEmail.isBlank()) {
                String normalizedGuestEmail = com.wisecartecommerce.ecommerce.util.EmailNormalizer.normalize(guestEmail);
                Long used = orderRepository.countByGuestEmailAndCouponCode(normalizedGuestEmail, coupon.getCode());
                if (used != null && used >= coupon.getMaxUsagePerUser()) {
                    throw new CustomException(
                            "You have already used this coupon the maximum number of times");
                }
            }
            if (coupon.getMaxUsagePerUser() != null && guestIp != null && !guestIp.isBlank()) {
                LocalDateTime since = LocalDateTime.now().minusHours(24);
                Long usedFromIp = orderRepository.countByGuestIpAndCouponCodeSince(
                        guestIp, coupon.getCode(), since);
                if (usedFromIp != null && usedFromIp >= coupon.getMaxUsagePerUser()) {
                    throw new CustomException(
                            "This coupon has already been used the maximum number of times from this network recently. Please try again later.");
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new CustomException("Coupon is not yet valid");
        }
        if (coupon.getExpirationDate() != null && now.isAfter(coupon.getExpirationDate())) {
            throw new CustomException("Coupon has expired");
        }

        if (coupon.getMaxUsageCount() != null
                && coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount()) {
            throw new CustomException("Coupon usage limit has been reached");
        }

        if (coupon.getMinimumPurchaseAmount() != null
                && subtotal.compareTo(coupon.getMinimumPurchaseAmount()) < 0) {
            throw new CustomException(
                    "Minimum purchase of ₱" + coupon.getMinimumPurchaseAmount()
                    + " required for this coupon");
        }

        int minQty = coupon.getMinimumProductQuantity() != null
                ? coupon.getMinimumProductQuantity() : 0;
        if (minQty > 0 && cartItems != null && !cartItems.isEmpty()) {
            Set<Long> applicable = coupon.getApplicableProducts();
            int qualifyingQty = cartItems.stream()
                    .filter(item -> applicable == null || applicable.isEmpty()
                    || applicable.contains(item.getProduct().getId()))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            if (qualifyingQty < minQty) {
                throw new CustomException(
                        "This coupon requires at least " + minQty
                        + " qualifying item(s) in your cart");
            }
        }

        // Per-user usage limit (skip for guests)
        if (userId != null && coupon.getMaxUsagePerUser() != null) {
            Integer used = couponUsageRepository.countByUserIdAndCouponId(userId, coupon.getId());
            if (used != null && used >= coupon.getMaxUsagePerUser()) {
                throw new CustomException(
                        "You have already used this coupon the maximum number of times");
            }
        }

        // Calculate discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean freeShipping = false;

        switch (coupon.getType()) {
            case PERCENTAGE -> {
                discountAmount = subtotal
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (coupon.getMaximumDiscountAmount() != null
                        && discountAmount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaximumDiscountAmount();
                }
            }
            case FIXED_AMOUNT -> {
                discountAmount = coupon.getDiscountValue().min(subtotal);
            }
            case FREE_SHIPPING -> {
                freeShipping = true;
                discountAmount = BigDecimal.ZERO;
            }
        }

        return CouponValidationResult.builder()
                .coupon(coupon)
                .discountAmount(discountAmount)
                .freeShipping(freeShipping)
                .build();
    }
}
