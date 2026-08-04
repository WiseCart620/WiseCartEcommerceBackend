-- V72__add_coupon_automatic_flag.sql
SET @dbname = DATABASE();
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='coupons' AND column_name='is_automatic') = 0,
  'ALTER TABLE coupons ADD COLUMN is_automatic BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;