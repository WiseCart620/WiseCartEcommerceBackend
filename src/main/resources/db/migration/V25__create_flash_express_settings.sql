-- V25__create_flash_express_settings.sql
CREATE TABLE flash_express_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mch_id VARCHAR(50) NOT NULL,
    secret_key VARCHAR(255) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    warehouse_no VARCHAR(50),
    src_name VARCHAR(100) NOT NULL,
    src_phone VARCHAR(20) NOT NULL,
    src_province_name VARCHAR(100) NOT NULL,
    src_city_name VARCHAR(100) NOT NULL,
    src_postal_code VARCHAR(10) NOT NULL,
    src_detail_address TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed with current values from yaml so nothing breaks on first boot
INSERT INTO flash_express_settings (
    mch_id, secret_key, base_url, warehouse_no,
    src_name, src_phone, src_province_name, src_city_name,
    src_postal_code, src_detail_address
) VALUES (
    'BA0097',
    'cde04a4490b14447f4b2f4966ba5d77a8d3fc7470243511aa00c75a269c55890',
    'https://open-api-tra.flashexpress.ph',
    '',
    'WiseCart',
    '09636026511',
    'Metro Manila',
    'Muntinlupa City',
    '1634',
    'Unit 81E, Udings Compound St. Bernadette College of Alabang East Service Road, Cupang Muntinlupa City, Metro Manila, Philippines 1780'
);