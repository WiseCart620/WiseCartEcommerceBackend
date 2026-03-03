package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HeroBannerResponse {
    private Long id;
    private String title;
    private String badge;
    private String subtitle;
    private String buttonText;
    private String buttonLink;
    private String imageUrl;
    private String textColor;
    private Integer overlayOpacity;
    private Integer displayOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}