-- V60__add_coupon_combinability.sql

ALTER TABLE coupons
    ADD COLUMN is_combinable TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE coupon_combinable_with (
    coupon_id            BIGINT NOT NULL,
    combinable_coupon_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, combinable_coupon_id),
    CONSTRAINT fk_ccw_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE CASCADE,
    CONSTRAINT fk_ccw_combinable
        FOREIGN KEY (combinable_coupon_id) REFERENCES coupons (id) ON DELETE CASCADE
);