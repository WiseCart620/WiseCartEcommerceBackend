package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FlashOrderResult {
    private String pno;
    private String sortCode;
    private String dstStoreName;
    private boolean upCountryCharge;
}