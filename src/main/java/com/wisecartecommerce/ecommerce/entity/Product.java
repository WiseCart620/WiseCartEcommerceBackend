package com.wisecartecommerce.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "sku", unique = true)
    private String sku;

    @Column(name = "upc", unique = true)
    private String upc;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "sold_count")
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * Stores multiple badge labels as a comma-separated string,
     * e.g. "New,Hot,Sale". Column length 500 supports ~40 badges.
     * Run: ALTER TABLE products MODIFY COLUMN label VARCHAR(500);  (MySQL)
     *   or ALTER TABLE products ALTER COLUMN label TYPE VARCHAR(500); (PostgreSQL)
     */
    @Column(length = 500)
    private String label;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    @Column(name = "lazada_url", length = 2048)
    private String lazadaUrl;

    @Column(name = "shopee_url", length = 2048)
    private String shopeeUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<ProductAddOn> addOns = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_recommendations",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "recommended_product_id"))
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Product> recommendedProducts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_category_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Category recommendationCategory;

    @Column(name = "weight_kg", precision = 8, scale = 3)
    private BigDecimal weightKg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("displayOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @ManyToMany(mappedBy = "wishlist", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> wishedBy = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariation> variations = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Computed helpers ───────────────────────────────────────────────────────

    public BigDecimal getDiscountedPrice() {
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            return price.subtract(price.multiply(discount.divide(BigDecimal.valueOf(100))));
        }
        return price;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /**
     * Returns weight in grams for Flash Express API calls.
     * Uses variation weight if available; falls back to product weight; then 500 g default.
     */
    public int getWeightGrams() {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return 500;
        }
        return weightKg.multiply(BigDecimal.valueOf(1000)).intValue();
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }

    public void addVariation(ProductVariation variation) {
        variations.add(variation);
        variation.setProduct(this);
    }

    // ── Image type filter methods ──────────────────────────────────────────────

    public List<ProductImage> getDescriptionImages() {
        if (images == null) return new ArrayList<>();
        return images.stream()
                .filter(img -> img.getImageType() == ProductImage.ImageType.DESCRIPTION)
                .collect(Collectors.toList());
    }

    public List<ProductImage> getGalleryImages() {
        if (images == null) return new ArrayList<>();
        return images.stream()
                .filter(img -> img.getImageType() == ProductImage.ImageType.GALLERY)
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .collect(Collectors.toList());
    }

    public Optional<ProductImage> getPrimaryGalleryImage() {
        return getGalleryImages().stream().filter(ProductImage::isPrimary).findFirst();
    }

    public boolean hasGalleryImages() {
        return !getGalleryImages().isEmpty();
    }

    public boolean hasDescriptionImages() {
        return !getDescriptionImages().isEmpty();
    }

    public int getGalleryImagesCount() {
        return getGalleryImages().size();
    }

    public int getDescriptionImagesCount() {
        return getDescriptionImages().size();
    }

    public void setPrimaryGalleryImage(Long imageId) {
        getGalleryImages().forEach(img -> img.setPrimary(img.getId().equals(imageId)));
        getPrimaryGalleryImage().ifPresent(img -> this.setImageUrl(img.getImageUrl()));
    }
}