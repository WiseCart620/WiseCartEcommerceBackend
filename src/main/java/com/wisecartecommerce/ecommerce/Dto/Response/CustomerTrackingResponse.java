package com.wisecartecommerce.ecommerce.Dto.Response;

import com.wisecartecommerce.ecommerce.util.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Combines Flash Express live tracking data with order-level info
 * the customer needs (status, courier phone for out-for-delivery).
 */
@Data
@Builder
public class CustomerTrackingResponse {

    // ── Order info ────────────────────────────────────────────────────────────
    private String orderNumber;
    private String trackingNumber;       // Flash PNO
    private String shippingCarrier;
    private OrderStatus orderStatus;     // internal status

    // ── Flash Express live tracking ───────────────────────────────────────────
    /** null = not yet picked up / no PNO assigned */
    private FlashTracking flashTracking;


    // ── Nested flash tracking ─────────────────────────────────────────────────
    @Data
    @Builder
    public static class FlashTracking {
        private String pno;
        private String origPno;
        private String returnedPno;
        private Integer state;
        private String stateText;
        private Long stateChangeAt;
        private List<FlashTrackingResponse.RouteEntry> routes;
    }
}