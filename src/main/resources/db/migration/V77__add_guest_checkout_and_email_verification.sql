-- V77__add_guest_checkout_and_email_verification.sql

-- ── Coupon: admin opt-in for guest checkout ─────────────────────────────
ALTER TABLE coupons
    ADD COLUMN allow_guest_checkout BOOLEAN NOT NULL DEFAULT FALSE;

-- ── Orders: capture guest IP for abuse-prevention on coupon usage ──────
ALTER TABLE orders
    ADD COLUMN guest_ip_address VARCHAR(45) NULL;

-- Indexes to support the per-email / per-IP coupon usage lookups
CREATE INDEX idx_orders_guest_email_coupon
    ON orders (guest_email, coupon_code);

CREATE INDEX idx_orders_guest_ip_coupon_created
    ON orders (guest_ip_address, coupon_code, created_at);

-- ── Guest email OTP verification ────────────────────────────────────────
CREATE TABLE guest_email_verifications (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    otp_hash       VARCHAR(255) NOT NULL,
    coupon_code    VARCHAR(100) NULL,
    attempts       INT NOT NULL DEFAULT 0,
    verified       BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at    DATETIME NULL,
    expires_at     DATETIME NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_guest_email_verifications_email (email),
    INDEX idx_guest_email_verifications_email_verified (email, verified, verified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;