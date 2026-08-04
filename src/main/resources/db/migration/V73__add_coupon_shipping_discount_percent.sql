-- V73__add_coupon_shipping_discount_percent.sql
ALTER TABLE coupons
    ADD COLUMN IF NOT EXISTS shipping_discount_percent INTEGER NOT NULL DEFAULT 100;