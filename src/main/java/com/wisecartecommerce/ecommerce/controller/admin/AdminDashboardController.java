package com.wisecartecommerce.ecommerce.controller.admin;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Dashboard", description = "Dashboard APIs for administrators")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object stats = dashboardService.getDashboardStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved", stats));
    }

    @GetMapping("/sales")
    @Operation(summary = "Get sales analytics")
    public ResponseEntity<ApiResponse<Object>> getSalesAnalytics(
            @RequestParam String period,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object analytics = dashboardService.getSalesAnalytics(period, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Sales analytics retrieved", analytics));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Get top selling products")
    public ResponseEntity<ApiResponse<Object>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object topProducts = dashboardService.getTopProducts(limit, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Top products retrieved", topProducts));
    }

    @GetMapping("/top-customers")
    @Operation(summary = "Get top customers")
    public ResponseEntity<ApiResponse<Object>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object topCustomers = dashboardService.getTopCustomers(limit, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Top customers retrieved", topCustomers));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue breakdown")
    public ResponseEntity<ApiResponse<Object>> getRevenueBreakdown(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object revenue = dashboardService.getRevenueBreakdown(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue breakdown retrieved", revenue));
    }

    @GetMapping("/orders/status")
    @Operation(summary = "Get order status distribution")
    public ResponseEntity<ApiResponse<Object>> getOrderStatusDistribution(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        Object distribution = dashboardService.getOrderStatusDistribution(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Order status distribution retrieved", distribution));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock alerts")
    public ResponseEntity<ApiResponse<Object>> getLowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold) {
        
        Object alerts = dashboardService.getLowStockAlerts(threshold);
        return ResponseEntity.ok(ApiResponse.success("Low stock alerts retrieved", alerts));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent activities")
    public ResponseEntity<ApiResponse<Object>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit) {
        
        Object activities = dashboardService.getRecentActivities(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent activities retrieved", activities));
    }
}