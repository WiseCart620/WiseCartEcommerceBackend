-- V70__jnt_shipping_and_carrier_toggles.sql
SET @dbname = DATABASE();

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='flash_enabled') = 0,
  'ALTER TABLE app_settings ADD COLUMN flash_enabled BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_enabled') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_enabled BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='jnt_shipping_rates' AND column_name='overweight_additional_fee') = 0,
  'ALTER TABLE jnt_shipping_rates ADD COLUMN overweight_additional_fee DECIMAL(10,2) NOT NULL DEFAULT 0', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='jnt_shipping_rates' AND column_name='min_weight_kg') > 0,
  'ALTER TABLE jnt_shipping_rates DROP COLUMN min_weight_kg', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='jnt_shipping_rates' AND column_name='max_weight_kg') > 0,
  'ALTER TABLE jnt_shipping_rates DROP COLUMN max_weight_kg', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='jnt_shipping_rates' AND column_name='additional_fee_per_kg_over_max') > 0,
  'ALTER TABLE jnt_shipping_rates DROP COLUMN additional_fee_per_kg_over_max', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;