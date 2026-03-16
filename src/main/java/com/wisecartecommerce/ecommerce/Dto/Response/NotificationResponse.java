package com.wisecartecommerce.ecommerce.Dto.Response;

import lombok.*;

import java.time.LocalDateTime;

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
}