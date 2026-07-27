
ALTER TABLE addresses
    ADD COLUMN barangay VARCHAR(255) NULL;


ALTER TABLE app_settings
    ADD COLUMN jnt_origin_province VARCHAR(100) NULL,
    ADD COLUMN jnt_origin_city VARCHAR(150) NULL;


UPDATE app_settings
SET jnt_origin_province = 'CEBU',
    jnt_origin_city = 'LAPU-LAPU CITY'
WHERE jnt_origin_province IS NULL;


ALTER TABLE jnt_shipping_rates
    ADD COLUMN destination_barangay VARCHAR(150) NULL AFTER destination_city;


DROP INDEX idx_jnt_rates_destination ON jnt_shipping_rates;

CREATE INDEX idx_jnt_rates_destination
    ON jnt_shipping_rates (destination_province, destination_city, destination_barangay);


ALTER TABLE orders
    ADD COLUMN jnt_tracking_status VARCHAR(30) NULL,
    ADD COLUMN jnt_picked_up_at DATETIME NULL;