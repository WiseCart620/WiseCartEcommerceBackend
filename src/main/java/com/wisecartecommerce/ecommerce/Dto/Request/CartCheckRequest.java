package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckRequest {
    
    private Boolean checkStock = true;
    
    private Boolean checkPrices = true;
    
    private Boolean checkCoupon = true;
    
    private Boolean checkShipping = false;
    
    private Long shippingAddressId;
    
    private String currency;
}