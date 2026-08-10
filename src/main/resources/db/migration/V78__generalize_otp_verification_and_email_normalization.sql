-- V78__generalize_otp_verification_and_email_normalization.sql

-- ── Orders: normalized guest email for abuse-resistant coupon caps ─────
ALTER TABLE orders
    ADD COLUMN guest_email_normalized VARCHAR(255) NULL;

-- Backfill existing rows (simple lowercase; historical rows won't get
-- full +tag/dot stripping applied retroactively — that's fine, this only
-- affects cap-checking going forward for new orders)
UPDATE orders
SET guest_email_normalized = LOWER(guest_email)
WHERE guest_email IS NOT NULL;

CREATE INDEX idx_orders_guest_email_normalized_coupon
    ON orders (guest_email_normalized, coupon_code);

-- ── Generalize guest_email_verifications into a reusable OTP table ─────
ALTER TABLE guest_email_verifications
    ADD COLUMN email_normalized VARCHAR(255) NULL,
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'GUEST_CHECKOUT';

UPDATE guest_email_verifications
SET email_normalized = LOWER(email)
WHERE email_normalized IS NULL;

ALTER TABLE guest_email_verifications
    MODIFY COLUMN email_normalized VARCHAR(255) NOT NULL;

CREATE INDEX idx_guest_email_verifications_normalized_purpose
    ON guest_email_verifications (email_normalized, purpose, verified, verified_at);