-- Add mobile_image_url column to hero_banners table for mobile-optimized banner images
ALTER TABLE hero_banners 
ADD COLUMN mobile_image_url VARCHAR(500) NULL 
COMMENT 'URL for mobile-optimized banner image (recommended: 600x800px for mobile devices)';