package com.wisecartecommerce.ecommerce.Dto.Response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JntEstimateResponse {

    private String originProvince;
    private String originCity;
    private String destinationProvince;
    private String destinationCity;
    private String destinationBarangay;
    private String serviceType;
    private String bagSize;
    private BigDecimal weightKg;
    private BigDecimal shippingFee;
    private BigDecimal itemAdditionalFee;
    private BigDecimal valuationFee;
    private BigDecimal codFee;
    private BigDecimal codFeeWithVat;
    private BigDecimal totalAmount;
}
