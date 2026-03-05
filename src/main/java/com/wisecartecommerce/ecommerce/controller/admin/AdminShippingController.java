package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashNotifyResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.entity.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/admin/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Shipping", description = "Flash Express shipping management for admins")
public class AdminShippingController {

    private final FlashExpressShippingService shippingService;
    private final OrderRepository orderRepository;

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

    @PostMapping("/cancel/{pno}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelByPno(@PathVariable String pno) {

        // 1. Cancel on Flash Express
        shippingService.cancelOrder(pno); // throws CustomException on failure

        // 2. Find the matching local order (by trackingNumber OR orderNumber)
        Map<String, Object> result = new HashMap<>();
        result.put("pno", pno);
        result.put("flashCancelled", true);

        Optional<Order> orderOpt = orderRepository.findByTrackingNumber(pno);
        if (orderOpt.isEmpty()) {
            // fallback: orderNumber was set to PNO by assignFlashOrderNumber
            orderOpt = orderRepository.findByOrderNumber(pno);
        }

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Only cancel if not already in a terminal state
            if (order.getStatus() != OrderStatus.CANCELLED
                    && order.getStatus() != OrderStatus.DELIVERED
                    && order.getStatus() != OrderStatus.RETURNED) {

                OrderStatus prev = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(java.time.LocalDateTime.now());

                // Restore stock for each item
                for (var item : order.getItems()) {
                    var product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    product.setSoldCount(Math.max(0, product.getSoldCount() - item.getQuantity()));
                }

                orderRepository.save(order);
                log.info("Admin cancelled order {} (prev={}) via PNO={}", order.getOrderNumber(), prev, pno);

                result.put("orderId", order.getId());
                result.put("orderNumber", order.getOrderNumber());
                result.put("previousStatus", prev.name());
                result.put("newStatus", "CANCELLED");
                result.put("localOrderCancelled", true);
            } else {
                result.put("orderId", order.getId());
                result.put("orderNumber", order.getOrderNumber());
                result.put("localOrderCancelled", false);
                result.put("reason", "Order already in terminal status: " + order.getStatus());
            }
        } else {
            log.warn("Admin cancelled Flash PNO={} but no matching local order found", pno);
            result.put("localOrderCancelled", false);
            result.put("reason", "No local order found for PNO");
        }

        return ResponseEntity.ok(ApiResponse.success("Flash Express order cancelled", result));
    }

    @Data
    public static class NotifyCourierRequest {
        private int estimateParcelNumber = 1;
        private String remark;
    }
}