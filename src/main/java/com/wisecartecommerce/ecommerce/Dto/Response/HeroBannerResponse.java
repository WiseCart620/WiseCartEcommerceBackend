package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String mobileImageUrl;

}