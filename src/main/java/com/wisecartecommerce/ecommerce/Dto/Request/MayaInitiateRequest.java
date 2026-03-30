package com.wisecartecommerce.ecommerce.Dto.Request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MayaInitiateRequest {
    // Shipping address — either a saved address ID or inline fields
    private Long shippingAddressId;
    private String firstName;
    private String lastName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;

    private BigDecimal shippingFee;
    private Integer expressCategory;
    private String notes;
}