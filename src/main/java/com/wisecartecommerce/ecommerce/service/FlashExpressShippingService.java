package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Response.FlashOrderResult;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashNotifyResponse;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.Order;

public interface FlashExpressShippingService {

    FlashShippingRateResponse estimateRate(Address dstAddress, int weightGrams, int expressCategory);

    FlashShippingRateResponse estimateRateManual(
            String dstProvince, String dstCity, String dstPostalCode,
            int weightGrams, int expressCategory);

    FlashOrderResult createOrder(Order order, Address shippingAddress,
            int weightGrams, int expressCategory);

    /** Download PDF label for a given PNO. Returns raw PDF bytes. */
    byte[] printLabel(String pno);

    /** Notify Flash courier to come pick up parcels from your warehouse. */
    FlashNotifyResponse notifyCourier(int estimateParcelNumber, String remark);

    /** Track a parcel by PNO. */
    FlashTrackingResponse trackOrder(String pno);

    /** Cancel a Flash Express order by PNO. */
    void cancelOrder(String pno);
}