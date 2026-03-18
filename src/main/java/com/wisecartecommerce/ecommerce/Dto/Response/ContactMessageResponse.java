package com.wisecartecommerce.ecommerce.Dto.Response;

import com.wisecartecommerce.ecommerce.entity.ContactMessage.ContactStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private ContactStatus status;
    private LocalDateTime createdAt;
    private List<ContactReplyResponse> replies;
    private long unreadReplies;
}