package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromoCardRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    private String description;

    private String buttonText;

    private String link;

    private String color;

    private Integer displayOrder;

    private boolean active = true;

    private Integer overlayOpacity;
}