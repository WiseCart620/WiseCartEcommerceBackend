package com.wisecartecommerce.ecommerce.controller.customer;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.MayaInitiateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.PaymentRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.MayaCheckoutService;
import com.wisecartecommerce.ecommerce.service.PaymentResponse;
import com.wisecartecommerce.ecommerce.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customer/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final MayaCheckoutService mayaCheckoutService;

    @PostMapping
    @Operation(summary = "Process payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment processed", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", response));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Request refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> requestRefund(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {
        PaymentResponse response = paymentService.requestRefund(paymentId, reason);
        return ResponseEntity.ok(ApiResponse.success("Refund requested", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get payment history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory() {
        List<PaymentResponse> payments = paymentService.getPaymentHistory();
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved", payments));
    }

    @GetMapping("/methods")
    @Operation(summary = "Get available payment methods")
    public ResponseEntity<ApiResponse<List<String>>> getAvailablePaymentMethods() {
        List<String> methods = paymentService.getAvailablePaymentMethods();
        return ResponseEntity.ok(ApiResponse.success("Payment methods retrieved", methods));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Payment webhook (for payment providers)")
    public ResponseEntity<Void> handlePaymentWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/maya/initiate")
    @Operation(summary = "Initiate Maya checkout (no order created yet)")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiateMayaCheckout(
            @RequestBody MayaInitiateRequest request) {
        String checkoutUrl = mayaCheckoutService.initiateMayaCheckout(request);
        return ResponseEntity.ok(ApiResponse.success("Maya checkout initiated",
                Map.of("checkoutUrl", checkoutUrl)));
    }

    @GetMapping("/maya/status")
    @Operation(summary = "Poll checkout status after returning from Maya")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMayaCheckoutStatus(
            @RequestParam String ref) {
        Map<String, Object> status = mayaCheckoutService.getCheckoutStatus(ref);
        return ResponseEntity.ok(ApiResponse.success("Checkout status", status));
    }
}
