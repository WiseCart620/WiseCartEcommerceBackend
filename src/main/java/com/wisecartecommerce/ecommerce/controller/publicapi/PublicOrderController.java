package com.wisecartecommerce.ecommerce.controller.publicapi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.Payment;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.service.MayaService;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.util.CouponValidationResult;
import com.wisecartecommerce.ecommerce.util.CouponValidator;
import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.util.PaymentStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/public/orders")
@RequiredArgsConstructor
@Tag(name = "Public Orders", description = "Guest checkout APIs")
@Slf4j
public class PublicOrderController {

    private final OrderService orderService;
    private final CouponValidator couponValidator;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MayaService mayaService;

    @PostMapping("/guest")
    @Operation(summary = "Place order as guest")
    public ResponseEntity<ApiResponse<OrderResponse>> createGuestOrder(
            @Valid @RequestBody GuestOrderRequest request) {
        OrderResponse response = orderService.createGuestOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/guest/{orderNumber}")
    @Operation(summary = "Track guest order")
    public ResponseEntity<ApiResponse<OrderResponse>> trackGuestOrder(
            @PathVariable String orderNumber,
            @RequestParam String email) {
        OrderResponse response = orderService.trackGuestOrder(orderNumber, email);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved", response));
    }

    @PostMapping("/validate-coupon")
    @Operation(summary = "Validate a coupon code for guest checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCoupon(
            @RequestBody Map<String, Object> body) {
        String couponCode = (String) body.get("couponCode");
        BigDecimal subtotal = new BigDecimal(body.get("subtotal").toString());

        // Build a lightweight item list for qty check
        List<CartItem> cartItems = null;
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List<?> rawItems && !rawItems.isEmpty()) {
            cartItems = new java.util.ArrayList<>();
            for (Object raw : rawItems) {
                if (raw instanceof Map<?, ?> m) {
                    Long productId = Long.parseLong(m.get("productId").toString());
                    int quantity = Integer.parseInt(m.get("quantity").toString());
                    Product p = new Product();
                    p.setId(productId);
                    CartItem ci = new CartItem();
                    ci.setProduct(p);
                    ci.setQuantity(quantity);

                    if (m.containsKey("variationId") && m.get("variationId") != null) {
                        Long variationId = Long.parseLong(m.get("variationId").toString());
                        com.wisecartecommerce.ecommerce.entity.ProductVariation variation
                                = new com.wisecartecommerce.ecommerce.entity.ProductVariation();
                        variation.setId(variationId);
                        ci.setVariation(variation);
                    }

                    cartItems.add(ci);
                }
            }
        }

        CouponValidationResult result = couponValidator.validate(couponCode, subtotal, null, cartItems);
        Map<String, Object> data = Map.of(
                "couponCode", result.getCoupon().getCode(),
                "discountAmount", result.getDiscountAmount(),
                "discountValue", result.getCoupon().getDiscountValue(),
                "freeShipping", result.isFreeShipping(),
                "type", result.getCoupon().getType());
        return ResponseEntity.ok(ApiResponse.success("Coupon is valid", data));
    }

    @PostMapping("/guest/payments/maya/initiate")
    @Operation(summary = "Initiate Maya payment for guest order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateGuestMayaPayment(
            @RequestBody Map<String, Object> request) {

        String orderNumber = (String) request.get("orderNumber");
        String email = (String) request.get("email");

        log.info("Initiating Maya payment for guest order: {} email: {}", orderNumber, email);

        // Verify guest order exists and belongs to this email
        Order order = orderRepository.findByOrderNumberAndGuestEmail(orderNumber, email)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Check if order is eligible for payment
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Order is not pending payment. Current status: " + order.getPaymentStatus());
        }

        // Check if payment method is Maya
        if (!"maya".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Order payment method is not set to Maya. Current method: " + order.getPaymentMethod());
        }

        try {
            String checkoutRef = "WC-" + orderNumber + "-" + System.currentTimeMillis();

            String successUrl = "http://localhost:3000/payment/success";
            String failureUrl = "http://localhost:3000/payment/failed";
            String cancelUrl = "http://localhost:3000/payment/cancelled";

            successUrl += "?ref=" + checkoutRef + "&guest=true&orderNumber=" + orderNumber + "&email=" + email;
            failureUrl += "?ref=" + checkoutRef + "&guest=true&orderNumber=" + orderNumber;
            cancelUrl += "?ref=" + checkoutRef + "&guest=true&orderNumber=" + orderNumber;

            Map<String, String> checkoutResult = mayaService.createCheckoutWithRedirects(
                    checkoutRef,
                    order.getFinalAmount(),
                    order.getGuestFirstName(),
                    order.getGuestLastName(),
                    order.getGuestPhone(),
                    order.getGuestEmail(),
                    successUrl,
                    failureUrl,
                    cancelUrl
            );

            String redirectUrl = checkoutResult.get("redirectUrl");
            String checkoutId = checkoutResult.get("checkoutId");

            log.info("Maya checkout created for guest order {}: checkoutId={}", orderNumber, checkoutId);

            // Create payment record
            Payment payment = paymentRepository.findByOrderId(order.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (payment == null) {
                payment = Payment.builder()
                        .order(order)
                        .transactionId(checkoutRef)
                        .amount(order.getFinalAmount())
                        .status(PaymentStatus.PENDING)
                        .paymentMethod("MAYA")
                        .paymentGateway("Maya")
                        .build();
            }

            payment.setMayaPaymentId(checkoutId);
            payment.setGatewayResponse("Checkout initiated at: " + java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("redirectUrl", redirectUrl);
            response.put("checkoutId", checkoutId);
            response.put("orderNumber", orderNumber);
            response.put("amount", order.getFinalAmount());

            return ResponseEntity.ok(ApiResponse.success("Maya payment initiated", response));

        } catch (Exception e) {
            log.error("Failed to initiate Maya payment for guest order {}: {}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    @GetMapping("/guest/payments/maya/status")
    @Operation(summary = "Check Maya payment status for guest order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGuestMayaPaymentStatus(
            @RequestParam String ref) {

        log.info("Checking Maya payment status for guest ref: {}", ref);

        try {
            // Find payment by transaction reference
            Payment payment = paymentRepository.findByTransactionId(ref)
                    .orElse(null);

            if (payment == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "PENDING");
                response.put("message", "Payment still processing");
                return ResponseEntity.ok(ApiResponse.success("Status retrieved", response));
            }

            String checkoutId = payment.getMayaPaymentId();
            String status = "PENDING";

            if (checkoutId != null) {
                status = mayaService.getCheckoutStatus(checkoutId);
                if (status == null) {
                    status = "PENDING";
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("orderId", payment.getOrder().getId());
            response.put("orderNumber", payment.getOrder().getOrderNumber());

            // If payment is completed, update order status
            if ("COMPLETED".equals(status) || "PAYMENT_SUCCESS".equals(status)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(LocalDateTime.now());

                Order order = payment.getOrder();
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setStatus(OrderStatus.PROCESSING);

                orderRepository.save(order);
                paymentRepository.save(payment);

                log.info("Payment completed for guest order: {}", order.getOrderNumber());
                response.put("status", "COMPLETED");
            }

            return ResponseEntity.ok(ApiResponse.success("Status retrieved", response));

        } catch (Exception e) {
            log.error("Failed to check guest payment status: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success("Status check completed", errorResponse));
        }
    }
}
