package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.config.FlashExpressProperties;
import com.wisecartecommerce.ecommerce.util.FlashExpressClient;
import com.wisecartecommerce.ecommerce.util.FlashExpressSignatureUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/shipping/webhook")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Shipping", description = "Flash Express webhook registration")
@Slf4j
public class AdminWebhookController {

    private final FlashExpressProperties props;
    private final FlashExpressClient client;

    /**
     * Register your webhook URL with Flash Express.
     *
     * webhookApiCode:
     *   0 = status updates (picked up, delivered, etc.)
     *   4 = detailed route events
     *
     * serviceCategory:
     *   1 = configure (enable)
     *   0 = close (disable)
     *
     * Example body:
     * {
     *   "url": "https://yourdomain.com/api/webhook/flash/status",
     *   "webhookApiCode": 0,
     *   "serviceCategory": 1
     * }
     */
    @PostMapping("/register")
    @Operation(summary = "Register webhook URL with Flash Express")
    public ResponseEntity<ApiResponse<Object>> registerWebhook(
            @RequestBody RegisterWebhookRequest req) {

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", props.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("serviceCategory", String.valueOf(req.getServiceCategory()));
        params.put("webhookApiCode", String.valueOf(req.getWebhookApiCode()));
        if (req.getUrl() != null && !req.getUrl().isBlank()) {
            params.put("url", req.getUrl());
        }
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, props.getSecretKey()));

        Map<String, Object> response = client.post(
                props.getBaseUrl() + "/open/v1/setting/web_hook_service", params);

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

    /**
     * View current webhook settings from Flash.
     * GET /admin/shipping/webhook/info
     */
    @GetMapping("/info")
    @Operation(summary = "Get current Flash Express webhook settings")
    public ResponseEntity<ApiResponse<Object>> getWebhookInfo() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", props.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, props.getSecretKey()));

        Map<String, Object> response = client.post(
                props.getBaseUrl() + "/gw/fda/open/standard/webhook/setting/infos", params);

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
        private int webhookApiCode = 0;   // 0=status, 4=routes
        private int serviceCategory = 1;  // 1=enable, 0=disable
    }
}