-- V72__add_coupon_automatic_flag.sql

ALTER TABLE coupons
    ADD COLUMN is_automatic BOOLEAN NOT NULL DEFAULT FALSE;