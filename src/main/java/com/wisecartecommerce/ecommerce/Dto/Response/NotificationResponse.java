package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
    private String imageUrl;
    private Integer totalItems;
}
