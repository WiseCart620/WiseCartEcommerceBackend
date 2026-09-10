package com.wisecartecommerce.ecommerce.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteSettingsRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteSettingsResponse;
import com.wisecartecommerce.ecommerce.service.InfoNoteSettingsService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/admin/info-note-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Info Note Settings", description = "Global default product info note & icon")
public class AdminInfoNoteSettingsController {

    private final InfoNoteSettingsService infoNoteSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<InfoNoteSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved", infoNoteSettingsService.getSettings()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<InfoNoteSettingsResponse>> update(
            @Valid @RequestBody InfoNoteSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settings updated", infoNoteSettingsService.updateSettings(request)));
    }

    @PostMapping("/icon")
    public ResponseEntity<ApiResponse<InfoNoteSettingsResponse>> uploadIcon(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Icon uploaded", infoNoteSettingsService.uploadIcon(file)));
    }

    @PostMapping("/apply-to-empty")
    public ResponseEntity<ApiResponse<Integer>> applyToProductsWithoutCustomNote() {
        int updated = infoNoteSettingsService.applyNoteToProductsWithoutCustomNote();
        return ResponseEntity.ok(ApiResponse.success(
                "Note applied to " + updated + " products without a custom note", updated));
    }

    @PostMapping("/overwrite-all")
    public ResponseEntity<ApiResponse<Integer>> overwriteAll() {
        int updated = infoNoteSettingsService.overwriteNoteOnAllProducts();
        return ResponseEntity.ok(ApiResponse.success(
                "Note force-applied to all " + updated + " products (including custom ones)", updated));
    }

    @GetMapping("/custom-note-count")
    public ResponseEntity<ApiResponse<Long>> customNoteCount() {
        return ResponseEntity.ok(ApiResponse.success("Count retrieved",
                infoNoteSettingsService.countProductsWithCustomNote()));
    }
}