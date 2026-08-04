-- V75__add_default_weight_grams_to_app_settings.sql
SET @dbname = DATABASE();
SET @sql = (SELECT IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema=@dbname AND table_name='app_settings' AND column_name='default_weight_grams') = 0,
  'ALTER TABLE app_settings ADD COLUMN default_weight_grams INTEGER NOT NULL DEFAULT 500', 'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;