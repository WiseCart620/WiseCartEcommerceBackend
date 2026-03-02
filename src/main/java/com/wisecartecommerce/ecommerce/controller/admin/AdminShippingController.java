package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashNotifyResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Shipping", description = "Flash Express shipping management for admins")
public class AdminShippingController {

    private final FlashExpressShippingService shippingService;

    /**
     * Download shipping label PDF for a given PNO.
     * GET /admin/shipping/label/{pno}
     */
    @GetMapping("/label/{pno}")
    @Operation(summary = "Download shipping label PDF for a Flash PNO")
    public ResponseEntity<byte[]> downloadLabel(@PathVariable String pno) {
        byte[] pdfBytes = shippingService.printLabel(pno);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "label-" + pno + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Notify Flash Express courier to come pick up parcels.
     * POST /admin/shipping/notify-courier
     *
     * Body: { "estimateParcelNumber": 5, "remark": "ASAP" }
     */
    @PostMapping("/notify-courier")
    @Operation(summary = "Notify Flash Express courier for pickup")
    public ResponseEntity<ApiResponse<FlashNotifyResponse>> notifyCourier(
            @RequestBody NotifyCourierRequest req) {

        FlashNotifyResponse response = shippingService.notifyCourier(
                req.getEstimateParcelNumber(),
                req.getRemark());

        return ResponseEntity.ok(ApiResponse.success("Courier notified successfully", response));
    }

    /**
     * Track a Flash Express order by PNO.
     * GET /admin/shipping/track/{pno}
     */
    @GetMapping("/track/{pno}")
    @Operation(summary = "Track a Flash Express parcel")
    public ResponseEntity<ApiResponse<FlashTrackingResponse>> trackOrder(@PathVariable String pno) {
        FlashTrackingResponse tracking = shippingService.trackOrder(pno);
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved", tracking));
    }

    /**
     * Cancel a Flash Express order by PNO.
     * POST /admin/shipping/cancel/{pno}
     */
    @PostMapping("/cancel/{pno}")
    @Operation(summary = "Cancel a Flash Express order")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable String pno) {
        shippingService.cancelOrder(pno);
        return ResponseEntity.ok(ApiResponse.success("Flash Express order cancelled", null));
    }

    @Data
    public static class NotifyCourierRequest {
        private int estimateParcelNumber = 1;
        private String remark;
    }
}