package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Request.CartCheckRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemUpdateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartMergeRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CouponApplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.CartResponse;
import com.wisecartecommerce.ecommerce.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse response = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", response));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        CartResponse response = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(@PathVariable Long itemId) {
        CartResponse response = cartService.removeFromCart(itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Apply coupon to cart")
    public ResponseEntity<ApiResponse<CartResponse>> applyCoupon(
            @Valid @RequestBody CouponApplyRequest request) {
        CartResponse response = cartService.applyCoupon(request);
        return ResponseEntity.ok(ApiResponse.success("Coupon applied", response));
    }

    @DeleteMapping("/remove-coupon")
    public ResponseEntity<ApiResponse<CartResponse>> removeCoupon(
            @RequestParam(required = false) String couponCode) {
        CartResponse response = cartService.removeCoupon(couponCode);
        return ResponseEntity.ok(ApiResponse.success("Coupon removed", response));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount() {
        Integer count = cartService.getCartItemCount();
        return ResponseEntity.ok(ApiResponse.success("Cart item count retrieved", count));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge guest cart with user cart")
    public ResponseEntity<ApiResponse<CartResponse>> mergeCart(
            @Valid @RequestBody CartMergeRequest request) {
        CartResponse response = cartService.mergeCart(request);
        return ResponseEntity.ok(ApiResponse.success("Cart merged", response));
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch update cart items")
    public ResponseEntity<ApiResponse<CartResponse>> batchUpdateCart(
            @Valid @RequestBody CartRequest request) {
        CartResponse response = cartService.batchUpdateCart(request);
        return ResponseEntity.ok(ApiResponse.success("Cart updated successfully", response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate cart before checkout")
    public ResponseEntity<ApiResponse<CartResponse>> validateCart(
            @Valid @RequestBody CartCheckRequest request) {
        CartResponse response = cartService.validateCart(request);
        return ResponseEntity.ok(ApiResponse.success("Cart validation completed", response));
    }

    @GetMapping("/shipping-estimate")
    @Operation(summary = "Estimate shipping costs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimateShipping(
            @RequestParam(required = false) Long addressId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String postalCode) {

        Map<String, Object> estimates = cartService.estimateShipping(addressId, country, postalCode);
        return ResponseEntity.ok(ApiResponse.success("Shipping estimates retrieved", estimates));
    }

    @GetMapping("/tax-estimate")
    @Operation(summary = "Estimate tax")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estimateTax(
            @RequestParam(required = false) Long addressId) {

        Map<String, Object> taxEstimate = cartService.estimateTax(addressId);
        return ResponseEntity.ok(ApiResponse.success("Tax estimate retrieved", taxEstimate));
    }

    @PostMapping("/save")
    @Operation(summary = "Save cart for later")
    public ResponseEntity<ApiResponse<Void>> saveCart(
            @RequestParam(required = false) String cartName) {
        cartService.saveCart(cartName);
        return ResponseEntity.ok(ApiResponse.success("Cart saved successfully", null));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get saved carts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSavedCarts() {
        List<Map<String, Object>> savedCarts = cartService.getSavedCarts();
        return ResponseEntity.ok(ApiResponse.success("Saved carts retrieved", savedCarts));
    }

    @PostMapping("/restore/{savedCartId}")
    @Operation(summary = "Restore saved cart")
    public ResponseEntity<ApiResponse<CartResponse>> restoreCart(@PathVariable Long savedCartId) {
        CartResponse response = cartService.restoreCart(savedCartId);
        return ResponseEntity.ok(ApiResponse.success("Cart restored successfully", response));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get cart summary")
    public ResponseEntity<ApiResponse<CartResponse>> getCartSummary() {
        CartResponse response = cartService.getCartSummary();
        return ResponseEntity.ok(ApiResponse.success("Cart summary retrieved", response));
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate cart totals")
    public ResponseEntity<ApiResponse<CartResponse>> recalculateCart() {
        CartResponse response = cartService.recalculateCart();
        return ResponseEntity.ok(ApiResponse.success("Cart recalculated", response));
    }
}
