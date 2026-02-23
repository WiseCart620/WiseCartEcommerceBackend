package com.wisecartecommerce.ecommerce.service;


import com.wisecartecommerce.ecommerce.Dto.Request.CartCheckRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemUpdateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartMergeRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CouponApplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CartResponse;

import java.util.List;
import java.util.Map;

public interface CartService {
    CartResponse getCart();
    CartResponse addToCart(CartItemRequest request);
    CartResponse updateCartItem(Long itemId, CartItemUpdateRequest request);
    CartResponse removeFromCart(Long itemId);
    CartResponse batchUpdateCart(CartRequest request);
    void clearCart();
    CartResponse applyCoupon(CouponApplyRequest request);
    CartResponse removeCoupon();
    CartResponse validateCart(CartCheckRequest request);
    Integer getCartItemCount();
    CartResponse mergeCart(CartMergeRequest request);
    Map<String, Object> estimateShipping(Long addressId, String country, String postalCode);
    Map<String, Object> estimateTax(Long addressId);
    void saveCart(String cartName);
    List<Map<String, Object>> getSavedCarts();
    CartResponse restoreCart(Long savedCartId);
    CartResponse recalculateCart();
    CartResponse getCartSummary();
}