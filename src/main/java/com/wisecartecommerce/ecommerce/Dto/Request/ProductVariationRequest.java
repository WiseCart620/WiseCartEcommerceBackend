package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariationRequest {

    private Long id;
    
    @NotBlank(message = "Variation name is required")
    private String name;

    private String sku;

    private String upc;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal discount;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
}