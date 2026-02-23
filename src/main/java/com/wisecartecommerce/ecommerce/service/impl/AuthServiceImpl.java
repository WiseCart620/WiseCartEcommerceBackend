package com.wisecartecommerce.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.LoginRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.RegisterRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.config.JwtService;
import com.wisecartecommerce.ecommerce.controller.auth.AuthenticationResponse;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.service.AuthService;
import com.wisecartecommerce.ecommerce.service.EmailService;
import com.wisecartecommerce.ecommerce.util.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    
    @Override
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already registered");
        }
        
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new CustomException("Phone number already registered");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .verificationToken(UUID.randomUUID().toString())
                .emailVerified(false)
                .enabled(true)
                .build();
        
        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(savedUser);
        
        String accessToken = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);
        
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(mapToUserResponse(savedUser))
                .build();
    }
    
    @Override
    public AuthenticationResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            log.info("User logged in: {}", user.getEmail());
            
            return AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationTime())
                    .user(mapToUserResponse(user))
                    .build();
                    
        } catch (Exception e) {
            throw new CustomException("Invalid email or password");
        }
    }
    
    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        
        if (email == null || !jwtService.isTokenValid(refreshToken)) {
            throw new CustomException("Invalid refresh token");
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found"));
        
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(mapToUserResponse(user))
                .build();
    }
    
    @Override
    public void logout(String token) {
        jwtService.invalidateToken(token);
        log.info("User logged out");
    }
    
    @Override
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException("User not authenticated");
        }
        
        // ✅ FIXED: Handle different principal types
        Object principal = authentication.getPrincipal();
        User user;
        
        if (principal instanceof User) {
            // Principal is already a User object
            user = (User) principal;
        } else if (principal instanceof String) {
            // Principal is a username/email string
            String email = (String) principal;
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException("User not found: " + email));
        } else if (principal instanceof UserDetails) {
            // Principal is UserDetails but not our User class
            String email = ((UserDetails) principal).getUsername();
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomException("User not found: " + email));
        } else {
            throw new CustomException("Invalid authentication principal type: " + principal.getClass().getName());
        }
        
        return mapToUserResponse(user);
    }
    
    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found with email: " + email));
        
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
        
        userRepository.save(user);
        
        // Send password reset email
        emailService.sendPasswordResetEmail(user, resetToken);
        
        log.info("Password reset token generated for user: {}", email);
    }
    
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new CustomException("Invalid or expired reset token"));
        
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new CustomException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        
        userRepository.save(user);
        
        log.info("Password reset for user: {}", user.getEmail());
    }
    
    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new CustomException("Invalid verification token"));
        
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        
        userRepository.save(user);
        
        log.info("Email verified for user: {}", user.getEmail());
    }
    
    @Override
    public boolean validateToken(String token) {
        return jwtService.isTokenValid(token);
    }
    
    private UserResponse mapToUserResponse(User user) {
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
                .createdAt(user.getCreatedAt())
                .build();
    }
}