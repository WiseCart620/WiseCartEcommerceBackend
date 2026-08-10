package com.wisecartecommerce.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "guest_email_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestEmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_normalized", nullable = false)
    private String emailNormalized;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "coupon_code")
    private String couponCode;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OtpPurpose purpose = OtpPurpose.GUEST_CHECKOUT;

    public enum OtpPurpose {
        GUEST_CHECKOUT,
        SIGNUP,
        PASSWORD_RESET
    }

    @Builder.Default
    @Column(nullable = false)
    private Integer attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
