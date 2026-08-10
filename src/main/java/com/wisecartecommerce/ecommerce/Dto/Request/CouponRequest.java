package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.wisecartecommerce.ecommerce.entity.Coupon;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    private String description;

    @NotNull(message = "Coupon type is required")
    private Coupon.CouponType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount value cannot be negative")
    private BigDecimal discountValue;

    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;
    private Integer maxUsageCount;
    private Integer maxUsagePerUser;
    private Integer minimumProductQuantity;
    @Builder.Default
    private Integer shippingDiscountPercent = 100;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required")
    private LocalDateTime expirationDate;

    @Builder.Default
    private Boolean active = true;
    private Set<Long> applicableProducts;
    private Set<Long> applicableCategories;
    @Builder.Default
    private Boolean combinable = false;
    @Builder.Default
    private Set<Long> combinableWith = new HashSet<>();
    @Builder.Default
    private Boolean automatic = false;

    @Builder.Default
    private Boolean allowGuestCheckout = false;

    @AssertTrue(message = "Discount value must be greater than 0")
    public boolean isDiscountValueValid() {
        if (type == Coupon.CouponType.FREE_SHIPPING) {
            return true;
        }
        return discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0;
    }
}
