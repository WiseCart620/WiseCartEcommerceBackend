package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variation_id")
    private ProductVariation variation;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void calculateSubtotal() {
        subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }

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
}