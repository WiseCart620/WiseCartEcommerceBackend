package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoCardResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private String buttonText;
    private String link;
    private String imageUrl;
    private String color;
    private Integer displayOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer overlayOpacity;
}