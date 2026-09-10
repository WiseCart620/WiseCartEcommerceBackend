-- V81__add_product_shipping_note.sql

-- ── Products: per-product shipping note shown on the product page ──
ALTER TABLE products
    ADD COLUMN shipping_note VARCHAR(500) NULL;