package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
@Tag(name = "Order Tracking", description = "Track orders via Flash Express")
public class OrderTrackingController {

    private final FlashExpressShippingService shippingService;
    private final OrderRepository orderRepository;

    /**
     * Track by order number (PNO or ORD...).
     * GET /customer/orders/{orderNumber}/track
     *
     * Works for both authenticated and guest users since order number is required.
     */
    @GetMapping("/{orderNumber}/track")
    @Operation(summary = "Track an order by order number")
    public ResponseEntity<ApiResponse<FlashTrackingResponse>> trackOrder(
            @PathVariable String orderNumber) {

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderNumber));

        String trackingNumber = order.getTrackingNumber();

        // Only Flash PNOs can be tracked (format: P + alphanumeric)
        if (trackingNumber == null || !trackingNumber.startsWith("P")) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Order is being processed, tracking not yet available", null));
        }

        FlashTrackingResponse tracking = shippingService.trackOrder(trackingNumber);
        return ResponseEntity.ok(ApiResponse.success("Tracking info retrieved", tracking));
    }
}