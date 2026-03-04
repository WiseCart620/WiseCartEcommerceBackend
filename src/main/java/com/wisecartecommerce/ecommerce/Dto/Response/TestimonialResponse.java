    package com.wisecartecommerce.ecommerce.Dto.Response;

    import lombok.*;
    import java.time.LocalDateTime;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public class TestimonialResponse {
        private Long id;
        private String customerName;
        private String customerTitle;
        private String avatarUrl;
        private String review;
        private Integer rating;
        private String productName;
        private Integer displayOrder;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }