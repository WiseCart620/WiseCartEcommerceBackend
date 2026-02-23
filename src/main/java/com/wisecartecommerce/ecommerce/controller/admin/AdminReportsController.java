package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Reports", description = "Reports and analytics APIs")
public class AdminReportsController {

    private final ReportsService reportsService;

    @GetMapping("/sales")
    @Operation(summary = "Get sales report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesReport(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Map<String, Object> report = reportsService.getSalesReport(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Sales report retrieved", report));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get revenue report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueReport(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Map<String, Object> report = reportsService.getRevenueReport(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Revenue report retrieved", report));
    }

    @GetMapping("/products")
    @Operation(summary = "Get product performance report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductReport(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        Map<String, Object> report = reportsService.getProductPerformanceReport(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Product report retrieved", report));
    }
}