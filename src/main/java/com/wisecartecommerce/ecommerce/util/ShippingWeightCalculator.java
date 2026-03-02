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

@Component
@RequiredArgsConstructor
public class ShippingWeightCalculator {

    private final ProductVariationRepository productVariationRepository;


    public static final int DEFAULT_ITEM_WEIGHT_GRAMS = 500;


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



    public int calculateRawWeightGrams(int weightGramsPerItem, int quantity) {
        return Math.max(weightGramsPerItem * quantity, DEFAULT_ITEM_WEIGHT_GRAMS);
    }
}