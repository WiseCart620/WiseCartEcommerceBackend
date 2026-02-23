package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
}