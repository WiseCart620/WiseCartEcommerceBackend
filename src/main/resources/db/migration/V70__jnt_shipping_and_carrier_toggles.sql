-- V70__jnt_shipping_and_carrier_toggles.sql

ALTER TABLE app_settings ADD COLUMN flash_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_settings ADD COLUMN jnt_enabled BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE jnt_shipping_rate ADD COLUMN overweight_additional_fee DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE jnt_shipping_rate DROP COLUMN min_weight_kg;
ALTER TABLE jnt_shipping_rate DROP COLUMN max_weight_kg;
ALTER TABLE jnt_shipping_rate DROP COLUMN additional_fee_per_kg_over_max;