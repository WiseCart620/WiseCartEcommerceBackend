package com.wisecartecommerce.ecommerce.Dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wisecartecommerce.ecommerce.util.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private String avatarUrl;
    private boolean emailVerified;
    private boolean enabled;
    private Map<String, Object> stats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}