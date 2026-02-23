package com.wisecartecommerce.ecommerce.Dto.Request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponApplyRequest {

    private String couponCode;


    @Builder.Default
    private Boolean validateOnly = false;
}