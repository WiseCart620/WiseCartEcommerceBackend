package com.wisecartecommerce.ecommerce.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.util.CouponValidator;
import com.wisecartecommerce.ecommerce.util.CouponValidationResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public/orders")
@RequiredArgsConstructor
@Tag(name = "Public Orders", description = "Guest checkout APIs")
public class PublicOrderController {

    private final OrderService orderService;
    private final CouponValidator couponValidator;

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
        List<com.wisecartecommerce.ecommerce.entity.CartItem> cartItems = null;
        Object itemsObj = body.get("items");
        if (itemsObj instanceof List<?> rawItems && !rawItems.isEmpty()) {
            cartItems = new java.util.ArrayList<>();
            for (Object raw : rawItems) {
                if (raw instanceof Map<?, ?> m) {
                    Long productId = Long.parseLong(m.get("productId").toString());
                    int quantity = Integer.parseInt(m.get("quantity").toString());
                    com.wisecartecommerce.ecommerce.entity.Product p
                            = new com.wisecartecommerce.ecommerce.entity.Product();
                    p.setId(productId);
                    com.wisecartecommerce.ecommerce.entity.CartItem ci
                            = new com.wisecartecommerce.ecommerce.entity.CartItem();
                    ci.setProduct(p);
                    ci.setQuantity(quantity);
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
}
