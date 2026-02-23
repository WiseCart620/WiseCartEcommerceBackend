package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequest {
    
    private String guestCartId;
    
    private String guestSessionId;
    
    private Boolean replaceExisting;
    
    private Boolean mergeConflicts;
}