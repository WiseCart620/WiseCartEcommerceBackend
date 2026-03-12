CREATE TABLE app_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vat_rate DECIMAL(5,4) NOT NULL DEFAULT 0.1200,
    free_shipping_threshold DECIMAL(10,2) NOT NULL DEFAULT 599.00,
    store_name VARCHAR(255),
    store_email VARCHAR(255),
    store_phone VARCHAR(100),
    updated_at DATETIME
);

INSERT INTO app_settings (vat_rate, free_shipping_threshold, store_name)
VALUES (0.12, 599.00, 'WiseCart');