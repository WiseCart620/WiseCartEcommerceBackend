package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Response.FlashExpressDTO;

public interface FlashExpressService {
    FlashExpressDTO.ShippingRateResponse estimateShippingRate(FlashExpressDTO.ShippingRateRequest request);
}