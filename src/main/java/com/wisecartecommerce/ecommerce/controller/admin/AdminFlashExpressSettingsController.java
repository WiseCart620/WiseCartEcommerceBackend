package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.service.FlashExpressSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/flash-express/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlashExpressSettingsController {

    private final FlashExpressSettingsService settingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<FlashExpressSettingsResponse>> getSettings() {
        FlashExpressSettings s = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved", toResponse(s)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<FlashExpressSettingsResponse>> updateSettings(
            @Valid @RequestBody FlashExpressSettingsRequest request) {

        FlashExpressSettings updated = settingsService.updateSettings(toEntity(request));
        return ResponseEntity.ok(ApiResponse.success("Settings updated successfully", toResponse(updated)));
    }

    // ── DTOs ────────────────────────────────────────────────────────────────

    @Data
    public static class FlashExpressSettingsRequest {
        @NotBlank private String mchId;
        @NotBlank private String secretKey;
        @NotBlank private String baseUrl;
        private String warehouseNo;
        @NotBlank private String srcName;
        @NotBlank private String srcPhone;
        @NotBlank private String srcProvinceName;
        @NotBlank private String srcCityName;
        @NotBlank private String srcPostalCode;
        @NotBlank private String srcDetailAddress;
    }

    @Data
    public static class FlashExpressSettingsResponse {
        private String mchId;
        private String secretKey;
        private String baseUrl;
        private String warehouseNo;
        private String srcName;
        private String srcPhone;
        private String srcProvinceName;
        private String srcCityName;
        private String srcPostalCode;
        private String srcDetailAddress;
        private String updatedAt;
    }

    // ── Mappers ─────────────────────────────────────────────────────────────

    private FlashExpressSettings toEntity(FlashExpressSettingsRequest r) {
        return FlashExpressSettings.builder()
                .mchId(r.getMchId())
                .secretKey(r.getSecretKey())
                .baseUrl(r.getBaseUrl())
                .warehouseNo(r.getWarehouseNo())
                .srcName(r.getSrcName())
                .srcPhone(r.getSrcPhone())
                .srcProvinceName(r.getSrcProvinceName())
                .srcCityName(r.getSrcCityName())
                .srcPostalCode(r.getSrcPostalCode())
                .srcDetailAddress(r.getSrcDetailAddress())
                .build();
    }

    private FlashExpressSettingsResponse toResponse(FlashExpressSettings s) {
        FlashExpressSettingsResponse res = new FlashExpressSettingsResponse();
        res.setMchId(s.getMchId());
        res.setSecretKey(s.getSecretKey());
        res.setBaseUrl(s.getBaseUrl());
        res.setWarehouseNo(s.getWarehouseNo());
        res.setSrcName(s.getSrcName());
        res.setSrcPhone(s.getSrcPhone());
        res.setSrcProvinceName(s.getSrcProvinceName());
        res.setSrcCityName(s.getSrcCityName());
        res.setSrcPostalCode(s.getSrcPostalCode());
        res.setSrcDetailAddress(s.getSrcDetailAddress());
        res.setUpdatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
        return res;
    }
}