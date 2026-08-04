-- V76__add_jnt_fee_settings_to_app_settings.sql
SET @dbname = DATABASE();

SET @sql = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_cod_fee_rate') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_cod_fee_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0275', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_valuation_fee_rate') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_valuation_fee_rate DECIMAL(6,4) NOT NULL DEFAULT 0.0100', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_valuation_fee_minimum') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_valuation_fee_minimum DECIMAL(10,2) NOT NULL DEFAULT 5.00', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_overweight_rate_per_kg') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_overweight_rate_per_kg DECIMAL(10,2) NOT NULL DEFAULT 70.00', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='jnt_overweight_base_fee') = 0,
  'ALTER TABLE app_settings ADD COLUMN jnt_overweight_base_fee DECIMAL(10,2) NOT NULL DEFAULT 15.00', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;