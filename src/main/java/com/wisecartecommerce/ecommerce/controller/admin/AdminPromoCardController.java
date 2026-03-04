package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Request.PromoCardRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.PromoCardResponse;
import com.wisecartecommerce.ecommerce.service.impl.PromoCardServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/promo-cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCardController {

    private final PromoCardServiceImpl promoCardService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoCardResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Promo cards retrieved", promoCardService.getAll()));
    }

    /**
     * Create with optional image in one multipart request.
     * Frontend sends: card = JSON blob, image = file (optional)
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<PromoCardResponse>> create(
            @RequestPart("card") String cardJson,
            @RequestPart(value = "image", required = false) MultipartFile image) throws Exception {
        PromoCardRequest req = objectMapper.readValue(cardJson, PromoCardRequest.class);
        return ResponseEntity.ok(ApiResponse.success("Promo card created", promoCardService.create(req, image)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCardResponse>> update(
            @PathVariable Long id,
            @RequestBody PromoCardRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Promo card updated", promoCardService.update(id, req)));
    }

    /** Upload / replace just the image for an existing card */
    @PostMapping("/{id}/image")
    public ResponseEntity<ApiResponse<PromoCardResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", promoCardService.uploadImage(id, file)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PromoCardResponse>> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", promoCardService.toggleActive(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        promoCardService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Promo card deleted", null));
    }
}