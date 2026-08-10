package com.wisecartecommerce.ecommerce.controller.publicapi;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.CouponResponse;
import com.wisecartecommerce.ecommerce.service.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/coupons")
@RequiredArgsConstructor
public class PublicCouponController {

    private final CouponService couponService;

    @GetMapping("/automatic")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAutomaticCoupons(
            @RequestParam BigDecimal subtotal) {
        List<CouponResponse> coupons = couponService.getEligibleAutomaticCoupons(subtotal, true);
        return ResponseEntity.ok(ApiResponse.success("Automatic coupons retrieved", coupons));
    }
}