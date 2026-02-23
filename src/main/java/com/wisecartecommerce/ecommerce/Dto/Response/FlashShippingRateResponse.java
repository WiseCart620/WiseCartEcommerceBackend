package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FlashShippingRateResponse {
    private BigDecimal shippingFee;        // PHP (converted from cents)
    private BigDecimal upCountryFee;       // PHP - remote area surcharge
    private BigDecimal codTransferFee;     // PHP - COD fee if applicable
    private boolean upCountry;             // is remote area?
    private String pricePolicyText;        // "By weight" or "By dimensions"
    private int expressCategory;           // 1=standard, 2=on-time, 4=bulky
    private String expressLabel;           // human-readable label
}