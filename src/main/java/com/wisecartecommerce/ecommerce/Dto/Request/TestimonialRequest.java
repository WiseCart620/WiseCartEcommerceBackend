package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TestimonialRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    private String customerName;

    @Size(max = 100)
    private String customerTitle;

    @NotBlank(message = "Review text is required")
    private String review;

    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 150)
    private String productName;

    private Integer displayOrder;
    private boolean active;
}