package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class JntShippingRateRequest {
    private String originProvince;
    private String originCity;
    private String destinationProvince;
    private String destinationCity;
    private String serviceType;
    private String bagSize;
    private BigDecimal minWeightKg;
    private BigDecimal maxWeightKg;
    private BigDecimal shippingFee;
    private BigDecimal itemAdditionalFee;
    private BigDecimal additionalFeePerKgOverMax;
    private Boolean active;
}