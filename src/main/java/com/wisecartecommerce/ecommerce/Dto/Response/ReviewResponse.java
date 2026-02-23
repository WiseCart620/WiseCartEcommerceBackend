package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private Long productId;
    private String productName;
    private Integer rating;
    private String comment;
    private Integer helpfulCount;
    private boolean verifiedPurchase;
    private boolean reported;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}