package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "is_addon")
    private boolean isAddon = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_product_id")
    @JsonIgnore
    private Product addonProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_product_add_on_id")
    @JsonIgnore
    private ProductAddOn addonProductAddOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_variation_id")
    @JsonIgnore
    private ProductVariation addonVariation;

    @Column(name = "addon_price", precision = 10, scale = 2)
    private BigDecimal addonPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "gift_wrap")
    @Builder.Default
    private Boolean giftWrap = false;

    @Column(name = "gift_message", columnDefinition = "TEXT")
    private String giftMessage;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "saved_for_later")
    @Builder.Default
    private Boolean savedForLater = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getSavings() {
        if (originalPrice != null && originalPrice.compareTo(price) > 0) {
            return originalPrice.subtract(price).multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variation_id")
    private ProductVariation variation;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (isAddon)
            return;
        if (product != null && price == null) {
            price = product.getDiscountedPrice();
            originalPrice = product.getPrice();
        }
    }

}