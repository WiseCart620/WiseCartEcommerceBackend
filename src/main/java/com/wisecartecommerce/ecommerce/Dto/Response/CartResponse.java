package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Long userId;
    private String sessionId;
    private List<CartItemResponse> items;
    private Integer itemCount;
    private Integer uniqueItemCount;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private String couponCode;
    private List<String> couponCodes;
    private String couponDescription;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private BigDecimal estimatedTax;
    private BigDecimal estimatedShipping;
    private Boolean isEligibleForFreeShipping;
    private BigDecimal freeShippingThreshold;
    private BigDecimal amountToFreeShipping;
    private Map<String, String> warnings;
    private Map<String, String> errors;
    private Boolean isValidForCheckout;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productSku;
        private String productImage;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Boolean isOnSale;
        private Integer quantity;
        private BigDecimal subtotal;
        private Integer maxQuantity;
        private Boolean inStock;
        private Integer stockQuantity;
        private Boolean giftWrap;
        private String giftMessage;
        private String notes;
        private List<String> warnings;
        private Boolean isAvailable;
        private LocalDateTime addedAt;
        private LocalDateTime updatedAt;
        private String variationName;
        private boolean isAddon;
        private Long addonProductId;
        private String addonProductName;
        private String addonVariationId;
        private String addonVariationName;
        private BigDecimal addonPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartSummary {
        private Integer totalItems;
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal shipping;
        private BigDecimal tax;
        private BigDecimal total;
        private BigDecimal savings;
    }
}