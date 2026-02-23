-- V12__add_product_weight_kg.sql
-- Flash Express: Add weight_kg to products table

-- 1. Add weight_kg to products
ALTER TABLE products
    ADD COLUMN weight_kg DECIMAL(8, 3) DEFAULT NULL;

-- Add comment separately
ALTER TABLE products 
    MODIFY COLUMN weight_kg DECIMAL(8, 3) DEFAULT NULL 
    COMMENT 'Weight in kg for Flash Express shipping. NULL uses 0.5 kg default per item.';

-- 2. Seed a sensible default for existing products
UPDATE products
SET weight_kg = 0.500
WHERE weight_kg IS NULL;

-- 3. Add dimension columns to product_variations
ALTER TABLE product_variations
    ADD COLUMN weight_kg DECIMAL(8, 3) DEFAULT NULL,
    ADD COLUMN height_cm DECIMAL(8, 2) DEFAULT NULL,
    ADD COLUMN width_cm DECIMAL(8, 2) DEFAULT NULL,
    ADD COLUMN length_cm DECIMAL(8, 2) DEFAULT NULL;

-- 4. Copy product weight to variations that have no weight of their own
UPDATE product_variations pv
INNER JOIN products p ON pv.product_id = p.id
SET pv.weight_kg = p.weight_kg
WHERE pv.weight_kg IS NULL
  AND p.weight_kg IS NOT NULL;