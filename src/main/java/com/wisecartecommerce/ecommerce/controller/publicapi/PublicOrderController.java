package com.wisecartecommerce.ecommerce.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/public/orders")
@RequiredArgsConstructor
@Tag(name = "Public Orders", description = "Guest checkout APIs")
public class PublicOrderController {

    private final OrderService orderService;

    @PostMapping("/guest")
    @Operation(summary = "Place order as guest")
    public ResponseEntity<ApiResponse<OrderResponse>> createGuestOrder(
            @Valid @RequestBody GuestOrderRequest request) {
        OrderResponse response = orderService.createGuestOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/guest/{orderNumber}")
    @Operation(summary = "Track guest order")
    public ResponseEntity<ApiResponse<OrderResponse>> trackGuestOrder(
            @PathVariable String orderNumber,
            @RequestParam String email) {
        OrderResponse response = orderService.trackGuestOrder(orderNumber, email);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", response));
    }
}