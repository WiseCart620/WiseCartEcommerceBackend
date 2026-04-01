package com.wisecartecommerce.ecommerce.util;

import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CouponValidator {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * Validates a coupon code against the given subtotal and cart items. Pass
     * userId = null for guest orders (skips per-user usage check). Pass
     * cartItems = null to skip the minimum product quantity check.
     *
     * @throws CustomException with a user-friendly message on any failure
     */
    public CouponValidationResult validate(String couponCode, BigDecimal subtotal,
            Long userId, List<CartItem> cartItems) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new CustomException("Coupon code is required");
        }

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode.toUpperCase())
                .orElseThrow(() -> new CustomException(
                "Coupon code '" + couponCode + "' is invalid or inactive"));

        LocalDateTime now = LocalDateTime.now();

        // Date range
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new CustomException("Coupon is not yet valid");
        }
        if (coupon.getExpirationDate() != null && now.isAfter(coupon.getExpirationDate())) {
            throw new CustomException("Coupon has expired");
        }

        // Global usage limit
        if (coupon.getMaxUsageCount() != null
                && coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount()) {
            throw new CustomException("Coupon usage limit has been reached");
        }

        // Minimum purchase amount
        if (coupon.getMinimumPurchaseAmount() != null
                && subtotal.compareTo(coupon.getMinimumPurchaseAmount()) < 0) {
            throw new CustomException(
                    "Minimum purchase of ₱" + coupon.getMinimumPurchaseAmount()
                    + " required for this coupon");
        }

        // Minimum product quantity
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
