package com.wisecartecommerce.ecommerce.util;

import com.wisecartecommerce.ecommerce.entity.Coupon;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CouponValidationResult {
    private Coupon coupon;
    private BigDecimal discountAmount;
    private boolean freeShipping;
}