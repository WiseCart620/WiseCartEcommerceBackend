-- V73__add_coupon_shipping_discount_percent.sql
SET @dbname = DATABASE();
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='coupons' AND column_name='shipping_discount_percent') = 0,
  'ALTER TABLE coupons ADD COLUMN shipping_discount_percent INTEGER NOT NULL DEFAULT 100', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;