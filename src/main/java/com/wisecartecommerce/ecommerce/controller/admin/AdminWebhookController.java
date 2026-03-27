package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.service.FlashExpressSettingsService;
import com.wisecartecommerce.ecommerce.util.FlashExpressClient;
import com.wisecartecommerce.ecommerce.util.FlashExpressSignatureUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/shipping/webhook")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Shipping", description = "Flash Express webhook registration")
@Slf4j
public class AdminWebhookController {

    private final FlashExpressSettingsService settingsService; // ✅ changed
    private final FlashExpressClient client;

    @PostMapping("/register")
    @Operation(summary = "Register webhook URL with Flash Express")
    public ResponseEntity<ApiResponse<Object>> registerWebhook(
            @RequestBody RegisterWebhookRequest req) {

        FlashExpressSettings s = settingsService.getSettings(); // ✅ get from DB

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());                     // ✅ now has value
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("serviceCategory", String.valueOf(req.getServiceCategory()));
        params.put("webhookApiCode", String.valueOf(req.getWebhookApiCode()));
        if (req.getUrl() != null && !req.getUrl().isBlank()) {
            params.put("url", req.getUrl());
        }
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey())); // ✅

        Map<String, Object> response = client.post(
                s.getBaseUrl() + "/open/v1/setting/web_hook_service", params); // ✅

        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code == 1) {
            log.info("Flash webhook registered: url={}, code={}", req.getUrl(), req.getWebhookApiCode());
            return ResponseEntity.ok(ApiResponse.success("Webhook registered successfully", response.get("data")));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to register webhook: " + response.get("message")));
        }
    }

    @GetMapping("/info")
    @Operation(summary = "Get current Flash Express webhook settings")
    public ResponseEntity<ApiResponse<Object>> getWebhookInfo() {
        FlashExpressSettings s = settingsService.getSettings(); // ✅

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        Map<String, Object> response = client.post(
                s.getBaseUrl() + "/gw/fda/open/standard/webhook/setting/infos", params);

        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code == 1) {
            return ResponseEntity.ok(ApiResponse.success("Webhook info retrieved", response.get("data")));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get webhook info: " + response.get("message")));
        }
    }

    @Data
    public static class RegisterWebhookRequest {
        private String url;
        private int webhookApiCode = 0;
        private int serviceCategory = 1;
    }
}
