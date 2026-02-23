package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariationResponse {
    private Long id;
    private String name;
    private String sku;
    private String upc;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal discountedPrice;
    private Integer stockQuantity;
    private boolean inStock;
    private String imageUrl;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
}