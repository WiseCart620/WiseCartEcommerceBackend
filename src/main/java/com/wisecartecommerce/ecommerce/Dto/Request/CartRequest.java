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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemRequest {
        
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        
        private String notes; // Optional notes for this specific item
        
        @Builder.Default
        private Boolean giftWrap = false;
        
        private String giftMessage;
    }
}