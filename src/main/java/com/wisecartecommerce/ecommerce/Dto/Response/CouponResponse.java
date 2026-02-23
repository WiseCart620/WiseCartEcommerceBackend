package com.wisecartecommerce.ecommerce.Dto.Response;

import com.wisecartecommerce.ecommerce.entity.Coupon;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private Coupon.CouponType type;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;
    private Integer maxUsageCount;
    private Integer currentUsageCount;
    private Integer maxUsagePerUser;
    private LocalDateTime startDate;
    private LocalDateTime expirationDate;
    private Boolean active;
    private Set<Long> applicableProducts;
    private Set<Long> applicableCategories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}