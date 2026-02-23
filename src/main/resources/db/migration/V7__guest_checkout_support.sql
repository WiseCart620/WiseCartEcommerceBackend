-- V7__guest_checkout_support.sql
-- Adds guest checkout support: guest fields on orders, nullable user on addresses

-- ─── 1. Add guest fields to orders table ─────────────────────────────────────
ALTER TABLE orders ADD COLUMN guest_email      VARCHAR(255) NULL;
ALTER TABLE orders ADD COLUMN guest_first_name VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN guest_last_name  VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN guest_phone      VARCHAR(50)  NULL;

-- ─── 2. Make user_id nullable on orders (guest orders have no account) ────────
ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL;

-- ─── 3. Make user_id nullable on addresses (guest addresses have no account) ──
ALTER TABLE addresses MODIFY COLUMN user_id BIGINT NULL;

-- ─── 4. Index guest_email for fast guest order lookups ────────────────────────
CREATE INDEX idx_orders_guest_email ON orders (guest_email);

-- ─── 5. Composite index for trackGuestOrder (orderNumber + guestEmail) ────────
CREATE INDEX idx_orders_order_number_guest_email ON orders (order_number, guest_email);