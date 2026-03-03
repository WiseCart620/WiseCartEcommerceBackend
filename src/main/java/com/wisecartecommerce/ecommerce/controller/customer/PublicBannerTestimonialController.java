package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.HeroBannerResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.TestimonialResponse;
import com.wisecartecommerce.ecommerce.service.impl.HeroBannerServiceImpl;
import com.wisecartecommerce.ecommerce.service.impl.TestimonialServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicBannerTestimonialController {

    private final HeroBannerServiceImpl bannerService;
    private final TestimonialServiceImpl testimonialService;

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<HeroBannerResponse>>> getBanners() {
        return ResponseEntity.ok(ApiResponse.success("Banners retrieved", bannerService.getActiveBanners()));
    }

    @GetMapping("/testimonials")
    public ResponseEntity<ApiResponse<List<TestimonialResponse>>> getTestimonials() {
        return ResponseEntity.ok(ApiResponse.success("Testimonials retrieved", testimonialService.getActive()));
    }
}