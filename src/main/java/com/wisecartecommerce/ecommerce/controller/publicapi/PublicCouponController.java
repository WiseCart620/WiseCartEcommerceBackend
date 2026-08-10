package com.wisecartecommerce.ecommerce.controller.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.AutomaticCouponsRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.CouponResponse;
import com.wisecartecommerce.ecommerce.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/coupons")
@RequiredArgsConstructor
public class PublicCouponController {

    private final CouponService couponService;

    @PostMapping("/automatic")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAutomaticCoupons(
            @RequestBody AutomaticCouponsRequest request) {
        List<CouponResponse> coupons = couponService.getEligibleAutomaticCoupons(
                request.getSubtotal(), true,
                request.getProductIds(), request.getCategoryIds(), request.getProductQuantities());
        return ResponseEntity.ok(ApiResponse.success("Automatic coupons retrieved", coupons));
    }
}