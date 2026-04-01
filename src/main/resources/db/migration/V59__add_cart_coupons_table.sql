-- V59__add_cart_coupons_table.sql

CREATE TABLE cart_coupons (
    cart_id BIGINT NOT NULL,
    coupon_code VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cart_coupons_cart
        FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO cart_coupons (cart_id, coupon_code)
SELECT id, coupon_code
FROM carts
WHERE coupon_code IS NOT NULL AND coupon_code != '';

ALTER TABLE carts DROP COLUMN coupon_code;