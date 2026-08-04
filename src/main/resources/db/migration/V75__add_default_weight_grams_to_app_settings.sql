-- V75__add_default_weight_grams_to_app_settings.sql
ALTER TABLE app_settings
    ADD COLUMN IF NOT EXISTS default_weight_grams INTEGER NOT NULL DEFAULT 500;