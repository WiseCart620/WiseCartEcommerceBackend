-- ============================================================
-- V8: Multi-image support + variation image persistence fix
-- ============================================================
-- What already exists from previous migrations:
--   V1  → products, product_images (base columns), product_images FK
--   V4  → product_variations (name, sku, upc, price, discount, stock, image_url, active)
--   V5  → product_variations: weight_kg, height_cm, width_cm, length_cm
--   V7  → guest checkout fields on orders/addresses
--
-- What V8 adds:
--   1. upc column on products table (for non-variation products)
--   2. Performance indexes for multi-image gallery queries
--   3. Safely ensure uq_variation_upc constraint exists
--   4. Backfill product_images from products.image_url where not yet migrated
-- ============================================================

-- ── 1. Add UPC to products (non-variation products can have their own UPC) ────
ALTER TABLE products
    ADD COLUMN upc VARCHAR(100) NULL AFTER sku;

-- ── 2. Performance indexes for multi-image support ────────────────────────────

-- Fast primary image lookup (used by mapToResponse to find primary image)
CREATE INDEX idx_product_images_primary
    ON product_images (product_id, is_primary);

-- Ordered gallery retrieval (used by the 10-image upload feature)
CREATE INDEX idx_product_images_order
    ON product_images (product_id, display_order);

-- ── 3. Ensure uq_variation_upc constraint exists on product_variations ────────
-- MySQL has no IF NOT EXISTS for constraints, so we check information_schema first.
SET @exist_upc := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = DATABASE()
      AND TABLE_NAME      = 'product_variations'
      AND CONSTRAINT_NAME = 'uq_variation_upc'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @sql_upc := IF(
    @exist_upc = 0,
    'ALTER TABLE product_variations ADD CONSTRAINT uq_variation_upc UNIQUE (upc)',
    'SELECT ''uq_variation_upc already exists, skipping'' AS info'
);
PREPARE stmt_upc FROM @sql_upc;
EXECUTE stmt_upc;
DEALLOCATE PREPARE stmt_upc;

-- Same check for uq_variation_sku (added in V4, but verify it's present)
SET @exist_sku := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA    = DATABASE()
      AND TABLE_NAME      = 'product_variations'
      AND CONSTRAINT_NAME = 'uq_variation_sku'
      AND CONSTRAINT_TYPE = 'UNIQUE'
);
SET @sql_sku := IF(
    @exist_sku = 0,
    'ALTER TABLE product_variations ADD CONSTRAINT uq_variation_sku UNIQUE (sku)',
    'SELECT ''uq_variation_sku already exists, skipping'' AS info'
);
PREPARE stmt_sku FROM @sql_sku;
EXECUTE stmt_sku;
DEALLOCATE PREPARE stmt_sku;

-- ── 4. Backfill product_images from products.image_url ───────────────────────
-- Safely seeds any products whose image_url hasn't been migrated yet.
-- Uses NOT EXISTS so it's safe to run even if partially migrated.
INSERT INTO product_images (product_id, image_url, is_primary, display_order, created_at)
SELECT
    p.id,
    p.image_url,
    TRUE,
    0,
    COALESCE(p.created_at, NOW())
FROM products p
WHERE p.image_url IS NOT NULL
  AND p.image_url <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM product_images pi
      WHERE pi.product_id = p.id
        AND pi.image_url  = p.image_url
  );