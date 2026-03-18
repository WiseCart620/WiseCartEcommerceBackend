package com.wisecartecommerce.ecommerce.Dto.Response;

import java.time.LocalDateTime;

import com.wisecartecommerce.ecommerce.entity.ContactReply.SenderType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContactReplyResponse {

    private Long id;
    private SenderType senderType;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
