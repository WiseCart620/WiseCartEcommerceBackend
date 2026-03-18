-- V37: Fix image_type column type to match Hibernate Enum

-- Drop the index we created in V36
DROP INDEX idx_product_images_image_type ON product_images;

-- Modify the column to use ENUM type
ALTER TABLE product_images 
MODIFY COLUMN image_type ENUM('GALLERY', 'DESCRIPTION') NOT NULL DEFAULT 'GALLERY';

-- Recreate the index
CREATE INDEX idx_product_images_image_type ON product_images(image_type);