package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartRequest {

    @Valid
    private List<CartItemRequest> items;

    private String couponCode;

    private Boolean clearExistingItems;

    private Long addonProductAddOnId;
    private Long addonVariationId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRequest {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        private Integer quantity;

        private String notes;

        @Builder.Default
        private Boolean giftWrap = false;

        private String giftMessage;
    }
}