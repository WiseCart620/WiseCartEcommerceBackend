package com.wisecartecommerce.ecommerce.Dto.Response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class JntRouteRateSummary {
    private String originProvince;
    private String originCity;
    private String destinationProvince;
    private String destinationCity;
    private String destinationBarangay;
    private String serviceType;

    private Long smallId;
    private BigDecimal smallFee;
    private BigDecimal smallItemFee;

    private Long mediumId;
    private BigDecimal mediumFee;
    private BigDecimal mediumItemFee;

    private Long bigId;
    private BigDecimal bigFee;
    private BigDecimal bigItemFee;

    private BigDecimal overweightAdditionalFee;
    private boolean active;
}