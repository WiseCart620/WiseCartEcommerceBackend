package com.wisecartecommerce.ecommerce.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.OrderItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShippingWeightCalculator {

    public static final int DEFAULT_ITEM_WEIGHT_GRAMS = 500;

    public int calculateCartWeightGrams(List<CartItem> items) {
        int total = 0;
        for (CartItem item : items) {
            total += resolveCartItemWeightGrams(item) * item.getQuantity();
        }
        return Math.max(total, DEFAULT_ITEM_WEIGHT_GRAMS);
    }

    private int resolveCartItemWeightGrams(CartItem item) {
        if (item.getVariation() != null) {
            int vw = item.getVariation().getWeightGrams();
            if (vw > 0) {
                return vw;
            }
        }
        return item.getProduct().getWeightGrams();
    }

    public int calculateOrderWeightGrams(List<OrderItem> items) {
        int total = 0;
        for (OrderItem item : items) {
            int w = 0;
            if (item.getVariation() != null) {
                w = item.getVariation().getWeightGrams();
            }
            if (w <= 0) {
                w = item.getProduct().getWeightGrams();
            }
            total += w * item.getQuantity();
        }
        return Math.max(total, DEFAULT_ITEM_WEIGHT_GRAMS);
    }

    public int calculateRawWeightGrams(int weightGramsPerItem, int quantity) {
        return Math.max(weightGramsPerItem * quantity, DEFAULT_ITEM_WEIGHT_GRAMS);
    }
}
