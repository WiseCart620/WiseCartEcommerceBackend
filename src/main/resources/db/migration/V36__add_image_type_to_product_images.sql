-- V36: Add image_type column to product_images table
-- This migration adds support for distinguishing between GALLERY and DESCRIPTION images

-- Step 1: Add the column as nullable first (to allow updates on existing records)
ALTER TABLE product_images ADD COLUMN image_type VARCHAR(50);

-- Step 2: Set default values for existing records
-- All existing images are assumed to be GALLERY images since description images
-- were not supported before this migration
UPDATE product_images SET image_type = 'GALLERY';

-- Note: If you had a separate way to identify description images (like a different table
-- or naming convention), you would update those records here. For example:
-- UPDATE product_images SET image_type = 'DESCRIPTION' 
-- WHERE image_url LIKE '%/description/%' OR image_url LIKE '%/descriptions/%';

-- Step 3: Now make the column NOT NULL
ALTER TABLE product_images MODIFY image_type VARCHAR(50) NOT NULL;

-- Step 4: Add an index for better query performance when filtering by image type
CREATE INDEX idx_product_images_image_type ON product_images(image_type);

-- Step 5: Add a comment to document the column (optional but recommended for production)
-- ALTER TABLE product_images MODIFY image_type VARCHAR(50) 
-- COMMENT 'Type of image: GALLERY (product display) or DESCRIPTION (rich text content)';

-- Step 6: Log the migration completion (optional)
-- This is just for documentation purposes
-- SELECT 'Added image_type column to product_images table' AS 'Migration Log';