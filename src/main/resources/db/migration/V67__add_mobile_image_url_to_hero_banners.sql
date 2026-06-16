-- Add mobile_image_url column to hero_banners table
ALTER TABLE hero_banners 
ADD COLUMN mobile_image_url VARCHAR(500) NULL;

-- Add comment for documentation
COMMENT ON COLUMN hero_banners.mobile_image_url IS 'URL for mobile-optimized banner image (recommended: 600x800px)';

-- Optional: Add index if you frequently query by mobile_image_url
-- CREATE INDEX idx_hero_banners_mobile_image ON hero_banners(mobile_image_url) WHERE mobile_image_url IS NOT NULL;