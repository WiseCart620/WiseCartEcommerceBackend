package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class JntRouteRateRequest {
    private String originProvince;
    private String originCity;
    private String destinationProvince;
    private String destinationCity;
    private String destinationBarangay;
    private String serviceType;

    private BigDecimal smallFee;
    private BigDecimal smallItemFee;
    private BigDecimal mediumFee;
    private BigDecimal mediumItemFee;
    private BigDecimal bigFee;
    private BigDecimal bigItemFee;

    private BigDecimal overweightAdditionalFee;
    private Boolean active;
}