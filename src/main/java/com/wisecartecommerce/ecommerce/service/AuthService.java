package com.wisecartecommerce.ecommerce.service;

import java.util.Map;

import com.wisecartecommerce.ecommerce.Dto.Request.LoginRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.RegisterRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.controller.auth.AuthenticationResponse;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse login(LoginRequest request);
    AuthenticationResponse refreshToken(String refreshToken);
    void logout(String token);
    UserResponse getCurrentUser();
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void verifyEmail(String token);
    boolean validateToken(String token);
    AuthenticationResponse socialLogin(Map<String, String> request);
}