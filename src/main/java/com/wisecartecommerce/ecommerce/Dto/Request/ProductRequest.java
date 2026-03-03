package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price must be less than 1,000,000")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 100, message = "SKU must be less than 100 characters")
    private String sku;

    @Size(max = 100, message = "UPC must be less than 100 characters")
    private String upc;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discount;

    @Size(max = 500, message = "Image URL must be less than 500 characters")
    private String imageUrl;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean featured = false;

    private List<ProductVariationRequest> variations;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    @Size(max = 30, message = "Label must be 30 characters or less")
    private String label;
}