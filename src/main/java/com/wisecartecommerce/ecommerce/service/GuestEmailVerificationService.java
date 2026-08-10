package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.entity.GuestEmailVerification;
import com.wisecartecommerce.ecommerce.entity.GuestEmailVerification.OtpPurpose;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.RateLimitException;
import com.wisecartecommerce.ecommerce.repository.GuestEmailVerificationRepository;
import com.wisecartecommerce.ecommerce.util.EmailNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestEmailVerificationService {

    private final GuestEmailVerificationRepository verificationRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;
    private static final int VERIFICATION_VALID_MINUTES = 30;

    @Transactional
    public void sendOtp(String rawEmail, String couponCode, OtpPurpose purpose) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new CustomException("Email is required");
        }
        String email = rawEmail.trim().toLowerCase();
        String normalized = EmailNormalizer.normalize(email);

        if (!rateLimitService.tryConsume(rateLimitService.otpBucket(normalized))) {
            throw new RateLimitException("Too many verification attempts. Please wait before trying again.");
        }

        verificationRepository.findFirstByEmailNormalizedAndPurposeOrderByCreatedAtDesc(normalized, purpose)
                .ifPresent(last -> {
                    long secondsSinceLast = ChronoUnit.SECONDS.between(last.getCreatedAt(), LocalDateTime.now());
                    if (secondsSinceLast < RESEND_COOLDOWN_SECONDS) {
                        throw new CustomException("Please wait a moment before requesting another code.");
                    }
                });

        String otp = generateOtp();
        GuestEmailVerification verification = GuestEmailVerification.builder()
                .email(email)
                .emailNormalized(normalized)
                .otpHash(ENCODER.encode(otp))
                .couponCode(couponCode != null ? couponCode.trim().toUpperCase() : null)
                .purpose(purpose)
                .attempts(0)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .build();
        verificationRepository.save(verification);

        // Send to the normalized address — for providers where that matters
        // (Gmail-style +tag/dot rules) it still lands in the same mailbox as
        // whatever variant the person typed, since those are provider-level
        // aliases pointing at one inbox. This also means we never spray OTPs
        // across a dozen invented variants of the same address.
        emailService.sendOtpEmail(normalized, otp);
        log.info("OTP requested [{}] for: {} (normalized: {})", purpose, email, normalized);
    }

    @Transactional
    public void verifyOtp(String rawEmail, String otp, OtpPurpose purpose) {
        if (rawEmail == null || rawEmail.isBlank() || otp == null || otp.isBlank()) {
            throw new CustomException("Email and code are required");
        }
        String normalized = EmailNormalizer.normalize(rawEmail.trim().toLowerCase());

        var candidates = verificationRepository.findRecentUnverifiedByNormalizedEmail(normalized, purpose);
        if (candidates.isEmpty()) {
            throw new CustomException("No verification code found for this email. Please request a new one.");
        }

        GuestEmailVerification latest = candidates.get(0);

        if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("This code has expired. Please request a new one.");
        }
        if (latest.getAttempts() >= MAX_ATTEMPTS) {
            throw new CustomException("Too many incorrect attempts. Please request a new code.");
        }

        if (!ENCODER.matches(otp.trim(), latest.getOtpHash())) {
            latest.setAttempts(latest.getAttempts() + 1);
            verificationRepository.save(latest);
            throw new CustomException("Incorrect code. Please try again.");
        }

        latest.setVerified(true);
        latest.setVerifiedAt(LocalDateTime.now());
        verificationRepository.save(latest);
        log.info("Email verified [{}]: {}", purpose, normalized);
    }

    @Transactional(readOnly = true)
    public boolean isEmailVerifiedRecently(String rawEmail, OtpPurpose purpose) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return false;
        }
        String normalized = EmailNormalizer.normalize(rawEmail.trim().toLowerCase());
        return verificationRepository.existsByEmailNormalizedAndPurposeAndVerifiedTrueAndVerifiedAtAfter(
                normalized, purpose, LocalDateTime.now().minusMinutes(VERIFICATION_VALID_MINUTES));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int code = RANDOM.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", code);
    }
}