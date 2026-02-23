package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GuestOrderRequest {

    // Guest contact info
    private String guestEmail;
    private String guestFirstName;
    private String guestLastName;
    private String guestPhone;

    // Shipping address fields (flat, not nested — kept for backward compat)
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;       // province in PH
    private String postalCode;
    private String country;

    // Order info
    private String paymentMethod;
    private String couponCode;
    private String notes;
    private List<GuestOrderItemRequest> items;

    // ── NEW: Flash Express shipping fields ─────────────────────────────────────
    /**
     * Pre-calculated shipping fee from /customer/shipping/estimate/manual.
     * If null, backend calculates via Flash API.
     */
    private BigDecimal shippingFee;

    /**
     * Express category: 1=Standard, 2=On-Time, 4=Bulky.
     * Used when backend needs to call Flash to calculate shipping.
     */
    private Integer expressCategory;

    @Data
    public static class GuestOrderItemRequest {
        private Long productId;
        private Integer quantity;
    }
}