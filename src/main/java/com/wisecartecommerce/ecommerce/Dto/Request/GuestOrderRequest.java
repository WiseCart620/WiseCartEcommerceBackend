package com.wisecartecommerce.ecommerce.Dto.Request;

import java.util.List;

import lombok.Data;

@Data
public class GuestOrderRequest {

    private String guestEmail;
    private String guestFirstName;
    private String guestLastName;
    private String guestPhone;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;

    private String paymentMethod;
    private String couponCode;
    private String notes;
    private List<GuestOrderItemRequest> items;
    private Integer expressCategory;

    @Data
    public static class GuestOrderItemRequest {

        private Long productId;
        private Integer quantity;
        private Long variationId;
    }
}
