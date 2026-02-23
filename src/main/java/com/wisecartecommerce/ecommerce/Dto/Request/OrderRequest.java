package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    private Long shippingAddressId;
    private AddressData shippingAddress;

    private Long billingAddressId;
    private AddressData billingAddress;

    private String paymentMethod;
    private String notes;
    private String couponCode;
    private BigDecimal shippingFee;

    private Integer expressCategory;

    @Data
    public static class AddressData {
        private String firstName;
        private String lastName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
        private String companyName;
    }
}