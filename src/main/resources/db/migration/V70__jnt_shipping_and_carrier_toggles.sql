-- V70__jnt_shipping_and_carrier_toggles.sql
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS flash_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_settings ADD COLUMN IF NOT EXISTS jnt_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE jnt_shipping_rates ADD COLUMN IF NOT EXISTS overweight_additional_fee DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE jnt_shipping_rates DROP COLUMN IF EXISTS min_weight_kg;
ALTER TABLE jnt_shipping_rates DROP COLUMN IF EXISTS max_weight_kg;
ALTER TABLE jnt_shipping_rates DROP COLUMN IF EXISTS additional_fee_per_kg_over_max;