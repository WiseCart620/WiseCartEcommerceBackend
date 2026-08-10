package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AutomaticCouponsRequest {
    private BigDecimal subtotal;
    private List<Long> productIds;
    private List<Long> categoryIds;
    private Map<Long, Integer> productQuantities;
}