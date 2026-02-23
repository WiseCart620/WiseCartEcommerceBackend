package com.wisecartecommerce.ecommerce.controller.customer;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/customer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order management APIs for customers")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get user's orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getUserOrders(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getUserOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", response));
    }

    @GetMapping("/{orderNumber}/track")
    @Operation(summary = "Track order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(@PathVariable String orderNumber) {
        OrderResponse response = orderService.trackOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Order tracking retrieved", response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelUserOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Request return")
    public ResponseEntity<ApiResponse<OrderResponse>> requestReturn(
            @PathVariable Long id,
            @RequestParam String reason) {
        OrderResponse response = orderService.requestReturn(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Return requested", response));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        
        List<OrderResponse> orders = orderService.getUserRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved", orders));
    }

    @GetMapping("/status/count")
    @Operation(summary = "Get order counts by status")
    public ResponseEntity<ApiResponse<Object>> getOrderCountsByStatus() {
        Object counts = orderService.getUserOrderCountsByStatus();
        return ResponseEntity.ok(ApiResponse.success("Order counts retrieved", counts));
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Create order review")
    public ResponseEntity<ApiResponse<OrderResponse>> createReview(
            @PathVariable Long id,
            @RequestParam String review,
            @RequestParam Integer rating) {
        OrderResponse response = orderService.createReview(id, review, rating);
        return ResponseEntity.ok(ApiResponse.success("Review submitted", response));
    }
}