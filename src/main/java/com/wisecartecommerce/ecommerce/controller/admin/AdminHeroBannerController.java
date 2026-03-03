package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Request.HeroBannerRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.HeroBannerResponse;
import com.wisecartecommerce.ecommerce.service.impl.HeroBannerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHeroBannerController {

    private final HeroBannerServiceImpl bannerService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HeroBannerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Banners retrieved", bannerService.getAllBanners()));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<HeroBannerResponse>> create(
            @RequestPart("banner") String bannerJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        HeroBannerRequest req = objectMapper.readValue(bannerJson, HeroBannerRequest.class);
        return ResponseEntity.ok(ApiResponse.success("Banner created", bannerService.create(req, image)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HeroBannerResponse>> update(
            @PathVariable Long id, @RequestBody HeroBannerRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Banner updated", bannerService.update(id, req)));
    }

    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<HeroBannerResponse>> uploadImage(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", bannerService.uploadImage(id, file)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HeroBannerResponse>> toggleStatus(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", bannerService.toggleActive(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted", null));
    }
}