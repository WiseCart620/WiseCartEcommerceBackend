package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashNotifyResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final OrderService orderService;

    @GetMapping("/label/{pno}")
    @Operation(summary = "Download shipping label PDF for a Flash PNO")
    public ResponseEntity<byte[]> downloadLabel(@PathVariable String pno) {
        byte[] labelBytes = shippingService.printLabel(pno);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"label-" + pno + ".pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(labelBytes.length)
                .body(labelBytes);
    }

    @PostMapping("/notify-courier")
    @Operation(summary = "Notify Flash Express courier for pickup")
    public ResponseEntity<ApiResponse<FlashNotifyResponse>> notifyCourier(
            @RequestBody NotifyCourierRequest req) {

        FlashNotifyResponse response = shippingService.notifyCourier(
                req.getEstimateParcelNumber(),
                req.getRemark());

        return ResponseEntity.ok(ApiResponse.success("Courier notified successfully", response));
    }

    @GetMapping("/track/{pno}")
    @Operation(summary = "Track a Flash Express parcel")
    public ResponseEntity<ApiResponse<FlashTrackingResponse>> trackOrder(@PathVariable String pno) {
        FlashTrackingResponse tracking = shippingService.trackOrder(pno);
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved", tracking));
    }

    @GetMapping("/notify-courier/parcel-count")
    @Operation(summary = "Get count of pending parcels ready for pickup")
    public ResponseEntity<ApiResponse<Integer>> getPendingParcelCount() {
        int count = shippingService.getPendingParcelCount();
        return ResponseEntity.ok(ApiResponse.success("Parcel count retrieved", count));
    }

    @PostMapping("/cancel/{pno}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelByPno(@PathVariable String pno) {

        shippingService.cancelOrder(pno);

        Map<String, Object> result = new HashMap<>();
        result.put("pno", pno);
        result.put("flashCancelled", true);

        Optional<Order> orderOpt = orderRepository.findByTrackingNumber(pno);
        if (orderOpt.isEmpty()) {
            orderOpt = orderRepository.findByOrderNumber(pno);
        }

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.getStatus() != OrderStatus.CANCELLED
                    && order.getStatus() != OrderStatus.DELIVERED
                    && order.getStatus() != OrderStatus.RETURNED) {

                OrderStatus prev = order.getStatus();
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(java.time.LocalDateTime.now());
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

    @GetMapping("/bulk-track")
    @Operation(summary = "Get live Flash states for all active orders in one call")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> bulkTrack() {

        List<Order> active = orderRepository.findActiveFlashOrders(
                List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED,
                        OrderStatus.RETURNED, OrderStatus.REFUNDED, OrderStatus.FAILED)
        );

        Map<String, Integer> result = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = active.stream()
                .map(order -> CompletableFuture.runAsync(() -> {
            try {
                FlashTrackingResponse flash = shippingService.trackOrder(order.getTrackingNumber());
                if (flash != null && flash.getState() != null) {
                    order.setFlashState(flash.getState());
                    orderRepository.save(order);
                    result.put(order.getOrderNumber(), flash.getState());
                    if (flash.getState() == 5 || flash.getState() == 7) {
                        orderService.syncFlashDeliveryStatus(order.getTrackingNumber());
                    }
                }
            } catch (Exception e) {
                log.warn("Bulk track failed for PNO={}: {}", order.getTrackingNumber(), e.getMessage());
            }
        }))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return ResponseEntity.ok(ApiResponse.success("Bulk track complete", result));
    }
}
