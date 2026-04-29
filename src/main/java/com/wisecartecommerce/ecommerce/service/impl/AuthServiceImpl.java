package com.wisecartecommerce.ecommerce.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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
import com.wisecartecommerce.ecommerce.exception.RateLimitException;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.service.AuthService;
import com.wisecartecommerce.ecommerce.service.EmailService;
import com.wisecartecommerce.ecommerce.service.RateLimitService;
import com.wisecartecommerce.ecommerce.util.Role;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;
    private final HttpServletRequest httpRequest;
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
        if (!rateLimitService.tryConsume(rateLimitService.loginBucket(getClientIp(httpRequest)))) {
            log.warn("Rate limit exceeded for login from IP: {}", getClientIp(httpRequest));
            throw new RateLimitException("Too many login attempts. Please try again in a minute.");
        }
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
    @Transactional
    public AuthenticationResponse socialLogin(Map<String, String> request) {
        String email = request.get("email");
        String name = request.get("name");
        String picture = request.get("picture");

        if (email == null) {
            throw new CustomException("Email not found from social login");
        }

        // Split name
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"User", ""};
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Find or create user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.CUSTOMER)
                    .emailVerified(true)
                    .enabled(true)
                    .avatarUrl(picture)
                    .build();
            return userRepository.save(newUser);
        });

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Social login successful for: {}", email);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .user(mapToUserResponse(user))
                .build();
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
        if (!rateLimitService.tryConsume(rateLimitService.forgotPasswordBucket(getClientIp(httpRequest)))) {
            throw new RateLimitException("Too many password reset requests. Please wait before trying again.");
        }
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

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
