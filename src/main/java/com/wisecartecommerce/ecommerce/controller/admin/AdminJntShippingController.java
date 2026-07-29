package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.JntEstimateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.JntShippingRateRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.JntEstimateResponse;
import com.wisecartecommerce.ecommerce.entity.JntShippingRate;
import com.wisecartecommerce.ecommerce.service.JntShippingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/jnt-shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin J&T Shipping", description = "Manage manually-configured J&T Express shipping rates")
public class AdminJntShippingController {

    private final JntShippingService jntShippingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary>>> getAllRates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Rates retrieved", jntShippingService.getGroupedRoutes(search, pageable)));
    }

    @PatchMapping("/route/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleRouteActive(@RequestBody Map<String, Object> body) {
        Long smallId = body.get("smallId") != null ? Long.valueOf(body.get("smallId").toString()) : null;
        Long mediumId = body.get("mediumId") != null ? Long.valueOf(body.get("mediumId").toString()) : null;
        Long bigId = body.get("bigId") != null ? Long.valueOf(body.get("bigId").toString()) : null;
        boolean active = Boolean.TRUE.equals(body.get("active"));
        jntShippingService.toggleRouteActive(smallId, mediumId, bigId, active);
        return ResponseEntity.ok(ApiResponse.success("Route status updated", null));
    }

    @DeleteMapping("/route")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @RequestParam(required = false) Long smallId,
            @RequestParam(required = false) Long mediumId,
            @RequestParam(required = false) Long bigId) {
        jntShippingService.deleteRoute(smallId, mediumId, bigId);
        return ResponseEntity.ok(ApiResponse.success("Route deleted", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JntShippingRate>> getRate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Rate retrieved", jntShippingService.getRateById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JntShippingRate>> createRate(@RequestBody JntShippingRateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Rate created", jntShippingService.createRate(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JntShippingRate>> updateRate(@PathVariable Long id, @RequestBody JntShippingRateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Rate updated", jntShippingService.updateRate(id, req)));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        jntShippingService.toggleActive(id, Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.ok(ApiResponse.success("Rate status updated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRate(@PathVariable Long id) {
        jntShippingService.deleteRate(id);
        return ResponseEntity.ok(ApiResponse.success("Rate deleted", null));
    }

    @PostMapping("/estimate")
    @Operation(summary = "Preview a shipping fee using currently configured rates")
    public ResponseEntity<ApiResponse<JntEstimateResponse>> testEstimate(@RequestBody JntEstimateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Estimate calculated", jntShippingService.estimate(req)));
    }

    @PostMapping("/route")
    @Operation(summary = "Save Small/Medium/Big bag fees + overweight surcharge for a route")
    public ResponseEntity<ApiResponse<Void>> saveRouteRates(
            @RequestBody com.wisecartecommerce.ecommerce.Dto.Request.JntRouteRateRequest req) {
        jntShippingService.saveRouteRates(req);
        return ResponseEntity.ok(ApiResponse.success("Route rates saved", null));
    }

    @GetMapping("/route")
    @Operation(summary = "Get the Small/Medium/Big bag rows for a route")
    public ResponseEntity<ApiResponse<List<JntShippingRate>>> getRouteRates(
            @RequestParam String originProvince, @RequestParam String originCity,
            @RequestParam String destinationProvince, @RequestParam String destinationCity,
            @RequestParam(required = false) String destinationBarangay) {
        return ResponseEntity.ok(ApiResponse.success("OK",
                jntShippingService.getRouteRates(originProvince, originCity, destinationProvince, destinationCity, destinationBarangay)));
    }

    @GetMapping("/locations/origin-provinces")
    public ResponseEntity<ApiResponse<List<String>>> originProvinces() {
        return ResponseEntity.ok(ApiResponse.success("OK", jntShippingService.getOriginProvinces()));
    }

    @GetMapping("/locations/origin-cities")
    public ResponseEntity<ApiResponse<List<String>>> originCities(@RequestParam String province) {
        return ResponseEntity.ok(ApiResponse.success("OK", jntShippingService.getOriginCities(province)));
    }

    @GetMapping("/locations/destination-provinces")
    public ResponseEntity<ApiResponse<List<String>>> destinationProvinces() {
        return ResponseEntity.ok(ApiResponse.success("OK", jntShippingService.getDestinationProvinces()));
    }

    @GetMapping("/locations/destination-cities")
    public ResponseEntity<ApiResponse<List<String>>> destinationCities(@RequestParam String province) {
        return ResponseEntity.ok(ApiResponse.success("OK", jntShippingService.getDestinationCities(province)));
    }
}
