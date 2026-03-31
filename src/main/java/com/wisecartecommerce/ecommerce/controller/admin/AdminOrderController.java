package com.wisecartecommerce.ecommerce.controller.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Orders", description = "Order management APIs for administrators")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders with filters")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String customerEmail) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getAllOrders(
                pageable, status, startDate, endDate, customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        String pno = response.getTrackingNumber();
        if (pno != null && pno.startsWith("P")
                && response.getStatus() != OrderStatus.DELIVERED
                && response.getStatus() != OrderStatus.CANCELLED
                && response.getStatus() != OrderStatus.RETURNED) {
            orderService.syncFlashDeliveryStatus(pno);
            response = orderService.getOrderById(id);
        }

        return ResponseEntity.ok(ApiResponse.success("Order retrieved", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String notes) {

        OrderResponse response = orderService.updateOrderStatus(id, status, notes);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get order statistics")
    public ResponseEntity<ApiResponse<Object>> getOrderStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Object stats = orderService.getOrderStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {

        List<OrderResponse> orders = orderService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent orders retrieved", orders));
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getTodayOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getTodayOrders(pageable);

        return ResponseEntity.ok(ApiResponse.success("Today's orders retrieved", orders));
    }

    @GetMapping("/customer/{userId}")
    @Operation(summary = "Get orders by customer")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrdersByCustomer(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponse> orders = orderService.getOrdersByCustomer(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("Customer orders retrieved", orders));
    }
}
