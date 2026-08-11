-- V79__add_product_display_order.sql

-- ── Products: manual display/sort order for customer-facing listings ──
ALTER TABLE products
    ADD COLUMN display_order INT NOT NULL DEFAULT 0;

-- Backfill: preserve current relative ordering (newest-first) as the
-- initial display_order so existing catalogs don't visually reshuffle
-- the moment this ships. Admins can then fine-tune from here.
SET @rownum := 0;
UPDATE products
SET display_order = (@rownum := @rownum + 1)
ORDER BY created_at DESC;

-- Composite index to support the common admin/storefront query shape:
-- WHERE active = ? ... ORDER BY display_order ASC, created_at DESC
CREATE INDEX idx_products_active_display_order
    ON products (active, display_order, created_at);