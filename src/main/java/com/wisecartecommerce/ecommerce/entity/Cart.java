package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    @Column(name = "session_id", unique = true)
    private String sessionId; // For guest carts
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("createdAt DESC")
    private List<CartItem> items = new ArrayList<>();
    
    @Column(name = "coupon_code")
    private String couponCode;
    
    @Column(name = "coupon_discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;
    
    @Column(name = "coupon_discount_percentage")
    private BigDecimal couponDiscountPercentage;
    
    @Column(name = "subtotal", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "shipping_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "total", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;
    
    @Column(name = "currency_code")
    @Builder.Default
    private String currencyCode = "PHP";
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_guest")
    @Builder.Default
    private Boolean isGuest = false;
    
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // For guest cart expiration
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public void calculateTotals() {
        // Calculate subtotal from active items (not saved for later)
        subtotal = items.stream()
                .filter(item -> !item.getSavedForLater())
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Apply coupon discount
        BigDecimal discount = couponDiscountAmount;
        if (couponDiscountPercentage != null && couponDiscountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.add(subtotal.multiply(couponDiscountPercentage.divide(BigDecimal.valueOf(100))));
        }
        
        discountAmount = discount;
        
        // Calculate total
        total = subtotal
                .subtract(discountAmount)
                .add(shippingAmount)
                .add(taxAmount);
        
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        
        lastActivity = LocalDateTime.now();
    }
    
    public Integer getItemCount() {
        return items.stream()
                .filter(item -> !item.getSavedForLater())
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    
    public Integer getUniqueItemCount() {
        return (int) items.stream()
                .filter(item -> !item.getSavedForLater())
                .map(CartItem::getProduct)
                .distinct()
                .count();
    }
    
    public CartItem getItemByProductId(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId) && !item.getSavedForLater())
                .findFirst()
                .orElse(null);
    }
    
    public void clearItems() {
        items.clear();
        calculateTotals();
    }
    
    public void clearCoupon() {
        couponCode = null;
        couponDiscountAmount = BigDecimal.ZERO;
        couponDiscountPercentage = null;
        calculateTotals();
    }
}