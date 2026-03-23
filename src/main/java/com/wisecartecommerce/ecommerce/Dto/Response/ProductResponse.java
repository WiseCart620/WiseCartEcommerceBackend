package com.wisecartecommerce.ecommerce.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.wisecartecommerce.ecommerce.entity.ProductImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;

    private BigDecimal price;
    private BigDecimal discountedPrice;

    private boolean hasVariations;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minDiscountedPrice;

    private List<ProductVariationResponse> variations;
    private List<ProductImage> descriptionImages;

    private Integer stockQuantity;
    private String upc;

    private Long categoryId;
    private String categoryName;
    private String sku;
    private String imageUrl;
    private List<ProductImageResponse> images;
    private ProductImageResponse primaryImage;
    private BigDecimal discount;
    private BigDecimal rating;
    private Integer reviewCount;
    private Integer soldCount;
    private Integer viewCount;
    private boolean active;
    private boolean featured;
    private boolean inStock;
    private Object reviewSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    // ── Multi-badge support ───────────────────────────────────────────────────
    private List<String> labels;

    private String lazadaUrl;
    private String shopeeUrl;
    private List<ProductAddOnResponse> addOns;
    private List<ProductSummaryResponse> recommendedProducts;
    private Long recommendationCategoryId;
    private String recommendationCategoryName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageResponse {

        private Long id;
        private String imageUrl;
        private boolean isPrimary;
        private Integer displayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAddOnResponse {

        private Long id;
        private Long addOnProductId;
        private String addOnProductName;
        private String addOnProductImage;
        private BigDecimal originalPrice;
        private BigDecimal specialPrice;
        private BigDecimal effectivePrice;
        private Integer discountPercent;
        private boolean inStock;
        private Integer displayOrder;
        private boolean hasVariations;
        private List<ProductVariationResponse> variations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummaryResponse {

        private Long id;
        private String name;
        private String imageUrl;
        private BigDecimal price;
        private BigDecimal discountedPrice;
        private boolean inStock;
        private BigDecimal rating;
        // ── Multi-badge support ───────────────────────────────────────────────
        private List<String> labels;
    }
}