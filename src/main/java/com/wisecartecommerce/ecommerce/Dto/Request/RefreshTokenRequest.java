package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    private Integer overlayOpacity;
}