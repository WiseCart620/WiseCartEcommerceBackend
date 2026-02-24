package com.wisecartecommerce.ecommerce.util;

import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Shared coupon validation used by both authenticated and guest order flows.
 */
@Component
@RequiredArgsConstructor
public class CouponValidator {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    /**
     * Validates a coupon code against the given subtotal.
     * Pass userId = null for guest orders (skips per-user usage check).
     *
     * @throws CustomException with a user-friendly message on any failure
     */
    public CouponValidationResult validate(String couponCode, BigDecimal subtotal, Long userId) {
        if (couponCode == null || couponCode.isBlank()) {
            throw new CustomException("Coupon code is required");
        }

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode.toUpperCase())
                .orElseThrow(() -> new CustomException("Coupon code '" + couponCode + "' is invalid or inactive"));

        LocalDateTime now = LocalDateTime.now();

        // Date range
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate()))
            throw new CustomException("Coupon is not yet valid");
        if (coupon.getExpirationDate() != null && now.isAfter(coupon.getExpirationDate()))
            throw new CustomException("Coupon has expired");

        // Global usage limit
        if (coupon.getMaxUsageCount() != null &&
                coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount())
            throw new CustomException("Coupon usage limit has been reached");

        // Minimum purchase
        if (coupon.getMinimumPurchaseAmount() != null &&
                subtotal.compareTo(coupon.getMinimumPurchaseAmount()) < 0)
            throw new CustomException(
                    "Minimum purchase of ₱" + coupon.getMinimumPurchaseAmount() + " required for this coupon");

        // Per-user usage limit (skip for guests)
        if (userId != null && coupon.getMaxUsagePerUser() != null) {
            Integer used = couponUsageRepository.countByUserIdAndCouponId(userId, coupon.getId());
            if (used != null && used >= coupon.getMaxUsagePerUser())
                throw new CustomException("You have already used this coupon the maximum number of times");
        }

        // Calculate discount amount
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean freeShipping = false;

        switch (coupon.getType()) {
            case PERCENTAGE -> {
                discountAmount = subtotal
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                // Apply cap if set
                if (coupon.getMaximumDiscountAmount() != null &&
                        discountAmount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaximumDiscountAmount();
                }
            }
            case FIXED_AMOUNT -> {
                discountAmount = coupon.getDiscountValue().min(subtotal); // never exceed subtotal
            }
            case FREE_SHIPPING -> {
                freeShipping = true;
                discountAmount = BigDecimal.ZERO; // shipping waived separately
            }
        }

        return CouponValidationResult.builder()
                .coupon(coupon)
                .discountAmount(discountAmount)
                .freeShipping(freeShipping)
                .build();
    }
}