package com.wisecartecommerce.ecommerce.util;

import org.springframework.stereotype.Component;

import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.entity.User;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .googleAccount(user.getFirebaseUid() != null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}