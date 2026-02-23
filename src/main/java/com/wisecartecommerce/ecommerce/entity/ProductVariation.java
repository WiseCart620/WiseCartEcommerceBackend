package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name; // e.g. "Red / XL", "Blue / M"

    @Column(unique = true)
    private String sku;

    @Column(unique = true)
    private String upc;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer stockQuantity;

    private String imageUrl;

    @Builder.Default
    private boolean active = true;

    // ── Dimensions (already present — used for Flash Express bulky shipping) ──
    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "height_cm", precision = 8, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "width_cm", precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Computed helpers ───────────────────────────────────────────────────────

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    public BigDecimal getDiscountedPrice() {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0)
            return price;
        BigDecimal discountAmount = price.multiply(discount).divide(BigDecimal.valueOf(100));
        return price.subtract(discountAmount);
    }

    /**
     * Returns this variation's weight in grams, or 0 if not set.
     * Caller should fall back to the parent Product's weight if this returns 0.
     */
    public int getWeightGrams() {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return weightKg.multiply(BigDecimal.valueOf(1000)).intValue();
    }
}