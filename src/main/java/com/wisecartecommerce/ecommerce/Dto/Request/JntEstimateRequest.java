package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class JntEstimateRequest {

    private String originProvince;
    private String originCity;
    private String destinationProvince;
    private String destinationCity;
    private String destinationBarangay;
    private BigDecimal weightKg;
    private BigDecimal declaredValue;
    private Boolean cod;
}
