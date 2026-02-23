-- ─────────────────────────────────────────────────────────────────────────────
-- Add dimension and weight columns to product_variations
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE product_variations
    ADD COLUMN weight_kg   DECIMAL(8, 2) NULL AFTER image_url,
    ADD COLUMN height_cm   DECIMAL(8, 2) NULL AFTER weight_kg,
    ADD COLUMN width_cm    DECIMAL(8, 2) NULL AFTER height_cm,
    ADD COLUMN length_cm   DECIMAL(8, 2) NULL AFTER width_cm;