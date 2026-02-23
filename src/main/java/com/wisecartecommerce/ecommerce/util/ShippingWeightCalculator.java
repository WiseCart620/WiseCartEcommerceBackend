package com.wisecartecommerce.ecommerce.util;

import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.OrderItem;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.repository.ProductVariationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Calculates total shipment weight from cart or order items.
 * Checks variation-level weight first, then product-level weight,
 * then falls back to DEFAULT_ITEM_WEIGHT_GRAMS.
 */
@Component
@RequiredArgsConstructor
public class ShippingWeightCalculator {

    private final ProductVariationRepository productVariationRepository;

    /** Fallback weight per line item when no weight data exists. */
    public static final int DEFAULT_ITEM_WEIGHT_GRAMS = 500;

    // ── Cart ──────────────────────────────────────────────────────────────────

    public int calculateCartWeightGrams(List<CartItem> items) {
        int total = 0;
        for (CartItem item : items) {
            total += resolveCartItemWeightGrams(item) * item.getQuantity();
        }
        return Math.max(total, DEFAULT_ITEM_WEIGHT_GRAMS);
    }

    private int resolveCartItemWeightGrams(CartItem item) {
        // Check variation weight first (if variation exists)
        if (item.getVariation() != null) {
            int vw = item.getVariation().getWeightGrams();
            if (vw > 0) return vw;
        }
        // Fall back to product weight
        return item.getProduct().getWeightGrams();
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    public int calculateOrderWeightGrams(List<OrderItem> items) {
        int total = 0;
        for (OrderItem item : items) {
            // Check if order item has variation
            if (item.getVariation() != null) {
                total += item.getVariation().getWeightGrams() * item.getQuantity();
            } else {
                // Fall back to product weight
                total += item.getProduct().getWeightGrams() * item.getQuantity();
            }
        }
        return Math.max(total, DEFAULT_ITEM_WEIGHT_GRAMS);
    }

    // ── Raw list of (weightGrams, quantity) pairs (for guest orders) ──────────

    public int calculateRawWeightGrams(int weightGramsPerItem, int quantity) {
        return Math.max(weightGramsPerItem * quantity, DEFAULT_ITEM_WEIGHT_GRAMS);
    }
}