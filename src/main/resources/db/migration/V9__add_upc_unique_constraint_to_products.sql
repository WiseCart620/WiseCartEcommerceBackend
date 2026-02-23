-- ============================================================
-- V9: Add UNIQUE constraint on products.upc
-- ============================================================
-- V8 added the upc column to products but without a unique
-- constraint. This migration adds it.
-- Note: MySQL automatically creates an index when a UNIQUE
-- constraint is added, so no separate CREATE INDEX is needed.
-- ============================================================

ALTER TABLE products
    ADD CONSTRAINT uq_products_upc UNIQUE (upc);