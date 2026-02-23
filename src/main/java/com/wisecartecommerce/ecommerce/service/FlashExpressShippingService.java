package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.entity.Address;

public interface FlashExpressShippingService {
    FlashShippingRateResponse estimateRate(Address dstAddress, int weightGrams, int expressCategory);
    FlashShippingRateResponse estimateRateManual(
            String dstProvince, String dstCity, String dstPostalCode,
            int weightGrams, int expressCategory);
}