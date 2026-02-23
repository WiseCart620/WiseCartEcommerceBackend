package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

public class FlashExpressDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingRateResponse {
        private BigDecimal estimatePrice;       // converted from cents to PHP
        private Integer pricePolicy;            // 1=weight, 2=dimensions
        private String pricePolicyText;
        private Boolean upCountry;
        private BigDecimal upCountryAmount;     // PHP
        private BigDecimal codTransferFee;      // PHP
        private Integer expressCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingRateRequest {
        private String srcProvinceName;
        private String srcCityName;
        private String srcDistrictName;
        private String srcPostalCode;
        private String dstProvinceName;
        private String dstCityName;
        private String dstDistrictName;
        private String dstPostalCode;
        private Integer weight;       // grams
        private Integer width;        // cm (optional)
        private Integer length;       // cm (optional)
        private Integer height;       // cm (optional)
        private Integer expressCategory; // 1=standard, 2=on-time, 4=bulky
    }

    // Raw response wrapper from Flash API
    @Data
    public static class FlashApiResponse<T> {
        private Integer code;
        private String message;
        private T data;
    }

    @Data
    public static class EstimateRateData {
        private String estimatePrice;
        private Integer pricePolicy;
        private Boolean upCountry;
        private Integer upCountryAmount;
        private Integer codTransferFee;
        private Integer expressCategory;
    }
}