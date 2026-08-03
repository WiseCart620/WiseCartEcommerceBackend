package com.wisecartecommerce.ecommerce.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.OrderItem;
import com.wisecartecommerce.ecommerce.repository.AppSettingsRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShippingWeightCalculator {

    private static final int FALLBACK_DEFAULT_ITEM_WEIGHT_GRAMS = 500;

    private final AppSettingsRepository appSettingsRepository;

    public int getDefaultItemWeightGrams() {
        return appSettingsRepository.findAll().stream()
                .findFirst()
                .map(s -> s.getDefaultWeightGrams() > 0 ? s.getDefaultWeightGrams() : FALLBACK_DEFAULT_ITEM_WEIGHT_GRAMS)
                .orElse(FALLBACK_DEFAULT_ITEM_WEIGHT_GRAMS);
    }

    public int calculateCartWeightGrams(List<CartItem> items) {
        int total = 0;
        for (CartItem item : items) {
            total += resolveCartItemWeightGrams(item) * item.getQuantity();
        }
        return Math.max(total, getDefaultItemWeightGrams());
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
        return Math.max(total, getDefaultItemWeightGrams());
    }

    public int calculateRawWeightGrams(int weightGramsPerItem, int quantity) {
        return Math.max(weightGramsPerItem * quantity, getDefaultItemWeightGrams());
    }

    /**
     * Cart weight in KG (2 decimals), for J&T's KG-based rate table.
     */
    public java.math.BigDecimal calculateCartWeightKg(List<CartItem> items) {
        int grams = calculateCartWeightGrams(items);
        return java.math.BigDecimal.valueOf(grams)
                .divide(java.math.BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);
    }
}
