package com.wisecartecommerce.ecommerce.controller.customer;

import java.math.BigDecimal;
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
import com.wisecartecommerce.ecommerce.Dto.Request.RefundRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.service.MayaCheckoutService;
import com.wisecartecommerce.ecommerce.service.OrderService;
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
    private final OrderService orderService;

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
        Map<String, String> result = mayaCheckoutService.initiateMayaCheckout(request);
        return ResponseEntity.ok(ApiResponse.success("Maya checkout initiated", result));
    }

    @GetMapping("/maya/status")
    @Operation(summary = "Poll checkout status after returning from Maya")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMayaCheckoutStatus(
            @RequestParam String ref,
            @RequestParam(required = false) String token) {
        Map<String, Object> status = mayaCheckoutService.getCheckoutStatus(ref, token);
        return ResponseEntity.ok(ApiResponse.success("Checkout status", status));
    }

    @PostMapping("/orders/{orderId}/refund")
    @Operation(summary = "Request Maya refund for cancelled or returned order")
    public ResponseEntity<ApiResponse<OrderResponse>> requestMayaRefund(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest request) {

        String reason = request != null ? request.getReason() : "Customer requested refund";
        BigDecimal amount = request != null ? request.getAmount() : null;

        OrderResponse response = orderService.requestMayaRefund(orderId, reason, amount);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated successfully", response));
    }

    /**
     * Void a Maya payment (for same-day cancellations)
     *
     * According to Maya docs: - Void is available only on the same day before
     * 11:59 PM GMT+8 - Full transaction only (no partial voids) - Hold amount
     * released instantly
     *
     * @param orderId The order ID to void
     * @param request Optional refund request with reason
     * @return Updated order response
     */
    @PostMapping("/orders/{orderId}/void")
    @Operation(summary = "Void Maya payment (same-day cancellation only)")
    public ResponseEntity<ApiResponse<OrderResponse>> requestMayaVoid(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest request) {

        String reason = request != null ? request.getReason() : "Customer requested cancellation";

        OrderResponse response = orderService.requestMayaVoid(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Void initiated successfully", response));
    }

    /**
     * Smart cancellation that automatically determines whether to void or
     * refund based on transaction date
     *
     * @param orderId The order ID to cancel
     * @param request Optional refund request with reason and amount
     * @return Updated order response
     */
    @PostMapping("/orders/{orderId}/cancel")
    @Operation(summary = "Cancel Maya payment (auto-selects void or refund based on date)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMayaPayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) RefundRequest request) {

        String reason = request != null ? request.getReason() : "Customer requested cancellation";
        BigDecimal amount = request != null ? request.getAmount() : null;

        // The OrderService should implement logic to determine if it's same day
        // and call the appropriate method (void for same day, refund for next day)
        OrderResponse response = orderService.cancelMayaPayment(orderId, reason, amount);
        return ResponseEntity.ok(ApiResponse.success("Cancellation initiated successfully", response));
    }
}
