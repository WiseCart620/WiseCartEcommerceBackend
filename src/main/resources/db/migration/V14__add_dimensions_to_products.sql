-- Add dimension fields to products table
ALTER TABLE products
    ADD COLUMN length_cm DECIMAL(10, 2) NULL,
    ADD COLUMN width_cm  DECIMAL(10, 2) NULL,
    ADD COLUMN height_cm DECIMAL(10, 2) NULL;