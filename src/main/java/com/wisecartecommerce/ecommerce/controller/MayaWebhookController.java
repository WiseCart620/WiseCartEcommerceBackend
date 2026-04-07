package com.wisecartecommerce.ecommerce.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.service.MayaCheckoutService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/webhooks/maya")
@RequiredArgsConstructor
@Slf4j
public class MayaWebhookController {

    private final MayaCheckoutService mayaCheckoutService;

    private static final List<String> ALLOWED_IPS = List.of(
            "13.229.160.234", "3.1.199.75", "18.138.50.235", "3.1.207.200"
    );

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String effectiveIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : ip;
        log.info("Maya webhook received - RemoteAddr={} X-Forwarded-For={} effectiveIp={}", ip, forwardedFor, effectiveIp);

        // TODO: Re-enable in production with proper IP check
        // if (!ALLOWED_IPS.contains(effectiveIp)) {
        //     log.warn("Maya webhook rejected from IP: {}", effectiveIp);
        //     return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        // }
        String status = (String) payload.get("paymentStatus");
        String ref = (String) payload.get("requestReferenceNumber");

        log.info("Maya webhook: status={} ref={}", status, ref);

        if (ref == null || ref.isBlank()) {
            log.warn("Maya webhook: missing ref");
            return ResponseEntity.ok().build();
        }

        try {
            if ("PAYMENT_SUCCESS".equals(status)) {
                mayaCheckoutService.handlePaymentSuccess(ref);
            } else if ("PAYMENT_FAILED".equals(status) || "PAYMENT_EXPIRED".equals(status)) {
                mayaCheckoutService.handlePaymentFailed(ref);
            } else if ("REFUND_SUCCESS".equals(status)) {
                log.info("Maya refund confirmed via webhook: ref={}", ref);
            } else if ("REFUND_FAILED".equals(status)) {
                log.error("Maya refund FAILED via webhook: ref={}", ref);
            }
        } catch (Exception e) {
            log.error("Error processing Maya webhook ref={}: {}", ref, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

}
