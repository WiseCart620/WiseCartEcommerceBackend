package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HeroBannerRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100)
    private String title;

    @Size(max = 100)
    private String badge;

    @Size(max = 200)
    private String subtitle;

    @Size(max = 50)
    private String buttonText;

    @Size(max = 255)
    private String buttonLink;

    @Pattern(regexp = "light|dark", message = "textColor must be 'light' or 'dark'")
    private String textColor;

    @Min(0) @Max(100)
    private Integer overlayOpacity;

    private Integer displayOrder;
    private boolean active;
}