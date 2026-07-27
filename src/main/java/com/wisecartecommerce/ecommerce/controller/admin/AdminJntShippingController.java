package com.wisecartecommerce.ecommerce.controller.admin;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/jnt-shipping")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin J&T Shipping", description = "Manage manually-configured J&T Express shipping rates")
public class AdminJntShippingController {

    private final JntShippingService jntShippingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<JntShippingRate>>> getAllRates(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("destinationProvince").ascending().and(Sort.by("minWeightKg").ascending()));
        return ResponseEntity.ok(ApiResponse.success("Rates retrieved", jntShippingService.getAllRates(search, pageable)));
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