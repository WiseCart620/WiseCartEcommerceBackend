package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactReplyRequest {

    @NotBlank(message = "Reply message is required")
    @Size(max = 5000)
    private String message;
}