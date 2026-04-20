package com.wisecartecommerce.ecommerce.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.PendingCheckoutRepository;
import com.wisecartecommerce.ecommerce.service.MayaCheckoutService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationController {

    private final MayaCheckoutService mayaCheckoutService;
    private final PendingCheckoutRepository pendingCheckoutRepository;

    @GetMapping("/failed-reason/{ref}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFailedReason(@PathVariable String ref) {
        Map<String, Object> result = new HashMap<>();
        try {
            var pending = pendingCheckoutRepository.findByCheckoutRef(ref).orElse(null);
            if (pending == null) {
                result.put("status", "NOT_FOUND");
                return ResponseEntity.ok(ApiResponse.success("Failed reason retrieved", result));
            }

            // If still PENDING, actively query Maya to get the real status + error code
            if (pending.getStatus() == com.wisecartecommerce.ecommerce.entity.PendingCheckout.PendingCheckoutStatus.PENDING
                    && pending.getMayaCheckoutId() != null) {
                try {
                    String mayaStatus = mayaCheckoutService.queryAndStoreMayaFailureReason(
                            pending.getCheckoutRef(), pending.getMayaCheckoutId());
                    log.info("Queried Maya for failed reason: ref={} mayaStatus={}", ref, mayaStatus);
                    // Reload after update
                    pending = pendingCheckoutRepository.findByCheckoutRef(ref).orElse(pending);
                } catch (Exception e) {
                    log.warn("Could not query Maya for failed reason ref={}: {}", ref, e.getMessage());
                }
            }

            result.put("status", pending.getStatus().name());
            if (pending.getErrorMessage() != null) {
                result.put("errorCode", pending.getErrorMessage());
            }
            log.info("Failed reason for ref {}: {}", ref, pending.getErrorMessage());

        } catch (Exception e) {
            log.error("Failed to get failed reason: {}", e.getMessage());
            result.put("status", "ERROR");
        }
        return ResponseEntity.ok(ApiResponse.success("Failed reason retrieved", result));
    }

    @GetMapping("/check-order/{ref}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIfOrderExists(@PathVariable String ref) {
        Map<String, Object> result = new HashMap<>();
        try {
            var pending = pendingCheckoutRepository.findByCheckoutRef(ref).orElse(null);

            if (pending == null) {
                result.put("orderExists", false);
                result.put("message", "No checkout found for this reference");
                return ResponseEntity.ok(ApiResponse.success("Check completed", result));
            }

            boolean orderExists = pending.getOrderId() != null && pending.getStatus() == com.wisecartecommerce.ecommerce.entity.PendingCheckout.PendingCheckoutStatus.COMPLETED;

            result.put("orderExists", orderExists);
            result.put("status", pending.getStatus().name());

            if (orderExists) {
                result.put("orderId", pending.getOrderId());
                result.put("message", "Order already exists for this payment");
                log.info("✅ Order exists for ref {}: orderId={}", ref, pending.getOrderId());
            } else {
                result.put("message", "No order found for this reference");
                log.info("No order exists for ref {}: status={}", ref, pending.getStatus());
            }

            return ResponseEntity.ok(ApiResponse.success("Check completed", result));

        } catch (Exception e) {
            log.error("Failed to check order existence for ref {}: {}", ref, e.getMessage());
            result.put("orderExists", false);
            result.put("message", "Error checking order status: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Check completed with error", result));
        }
    }

    @GetMapping("/check-payment-status/{ref}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(@PathVariable String ref) {
        Map<String, Object> result = new HashMap<>();
        try {
            var pending = pendingCheckoutRepository.findByCheckoutRef(ref).orElse(null);

            if (pending == null) {
                result.put("success", false);
                result.put("status", "NOT_FOUND");
                result.put("message", "No checkout found for this reference");
                return ResponseEntity.ok(ApiResponse.success("Check completed", result));
            }

            // Check actual payment status from Maya
            String mayaStatus = mayaCheckoutService.queryAndStoreMayaFailureReason(ref, pending.getMayaCheckoutId());

            boolean isActuallySuccessful = "COMPLETED".equals(mayaStatus) || "PAYMENT_SUCCESS".equals(mayaStatus);
            boolean isFailed = "FAILED".equals(mayaStatus) || "CANCELLED".equals(mayaStatus) || "EXPIRED".equals(mayaStatus);

            result.put("success", isActuallySuccessful);
            result.put("status", mayaStatus != null ? mayaStatus : pending.getStatus().name());
            result.put("isVerified", isActuallySuccessful);
            result.put("orderId", pending.getOrderId());

            // Add explicit failure flag for frontend
            result.put("isFailed", isFailed);

            if (pending.getErrorMessage() != null) {
                result.put("message", pending.getErrorMessage());
            }

            log.info("Payment status check for ref {}: success={}, status={}, isFailed={}",
                    ref, isActuallySuccessful, mayaStatus, isFailed);

            return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", result));

        } catch (Exception e) {
            log.error("Failed to check payment status for ref {}: {}", ref, e.getMessage());
            result.put("success", false);
            result.put("status", "ERROR");
            result.put("isFailed", true);
            result.put("message", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Check completed with error", result));
        }
    }
    

    @GetMapping("/verify/{ref}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @PathVariable String ref,
            @RequestParam(required = false) String token) {

        log.info("🔐 Payment verification request received for ref: {}", ref);

        // ✅ Token validation is handled by MayaCheckoutService.getCheckoutStatus
        // which checks against the verificationToken stored in the PendingCheckout entity
        log.info("Verifying payment for ref: {} with token: {}", ref, token != null ? "present" : "missing");

        // Don't validate token here - let the service handle it
        try {
            Map<String, Object> status = mayaCheckoutService.getCheckoutStatus(ref, token);
            boolean isVerified = "COMPLETED".equals(status.get("status"));
            status.put("verified", isVerified);
            status.put("checkoutRef", ref);

            if (isVerified) {
                log.info("✅ Payment verified successfully for ref: {}", ref);
                return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", status));
            } else {
                log.warn("⚠️ Payment not completed for ref: {} - Status: {}", ref, status.get("status"));
                return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", status));
            }

        } catch (CustomException e) {
            log.error("❌ Payment verification failed for ref {}: {}", ref, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("verified", false);
            errorResponse.put("status", "FAILED");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("checkoutRef", ref);
            return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", errorResponse));
        } catch (Exception e) {
            log.error("❌ Payment verification failed for ref {}: {}", ref, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("verified", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "VERIFICATION_FAILED|" + e.getMessage());
            errorResponse.put("checkoutRef", ref);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Payment verification failed: " + e.getMessage()));
        }
    }

    // Token generation is now handled in MayaCheckoutService using the database
    // This method is no longer used
    // public String generateVerificationToken(String ref) {
    //     String token = UUID.randomUUID().toString();
    //     return token;
    // }
    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        log.info("✅✅✅ PaymentVerificationController is LOADED and WORKING! ✅✅✅");
        return ResponseEntity.ok(ApiResponse.success("Controller is working!", "OK"));
    }
}
