package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AddressRepository;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Flash Express shipping fee estimation")
public class ShippingController {

    private final FlashExpressShippingService shippingService;
    private final AddressRepository addressRepository;

    /** Estimate using a saved address ID (logged-in users) */
    @PostMapping("/estimate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Estimate shipping fee for a saved address")
    public ResponseEntity<ApiResponse<FlashShippingRateResponse>> estimate(
            @RequestBody EstimateByAddressRequest req) {

        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + req.getAddressId()));

        int expressCategory = req.getExpressCategory() != null ? req.getExpressCategory() : 1;
        FlashShippingRateResponse rate = shippingService.estimateRate(address, req.getWeightGrams(), expressCategory);

        return ResponseEntity.ok(ApiResponse.success("Shipping rate retrieved", rate));
    }

    /** Estimate using raw fields — for guest checkout or checkout form before address is saved */
    @PostMapping("/estimate/manual")
    @Operation(summary = "Estimate shipping fee from address fields")
    public ResponseEntity<ApiResponse<FlashShippingRateResponse>> estimateManual(
            @RequestBody ManualEstimateRequest req) {

        int expressCategory = req.getExpressCategory() != null ? req.getExpressCategory() : 1;
        FlashShippingRateResponse rate = shippingService.estimateRateManual(
                req.getDstProvinceName(),
                req.getDstCityName(),
                req.getDstPostalCode(),
                req.getWeightGrams(),
                expressCategory
        );

        return ResponseEntity.ok(ApiResponse.success("Shipping rate retrieved", rate));
    }

    @Data
    public static class EstimateByAddressRequest {
        private Long addressId;
        private int weightGrams;        // total weight of order in grams
        private Integer expressCategory; // 1=standard, 2=on-time, 4=bulky
    }

    @Data
    public static class ManualEstimateRequest {
        private String dstProvinceName;
        private String dstCityName;
        private String dstPostalCode;
        private int weightGrams;
        private Integer expressCategory;
    }
}