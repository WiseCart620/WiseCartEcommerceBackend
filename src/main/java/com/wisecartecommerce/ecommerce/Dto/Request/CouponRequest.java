package com.wisecartecommerce.ecommerce.Dto.Request;

import com.wisecartecommerce.ecommerce.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountValue;

    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;
    private Integer maxUsageCount;
    private Integer maxUsagePerUser;
    private Integer minimumProductQuantity;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "Expiration date is required")
    private LocalDateTime expirationDate;

    private Boolean active = true;
    private Set<Long> applicableProducts;
    private Set<Long> applicableCategories;
    private Boolean combinable = false;
    private Set<Long> combinableWith = new HashSet<>();
}
