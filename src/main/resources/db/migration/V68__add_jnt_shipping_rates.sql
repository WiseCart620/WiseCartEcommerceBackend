
CREATE TABLE jnt_shipping_rates (
    id                            BIGINT AUTO_INCREMENT PRIMARY KEY,

    origin_province               VARCHAR(100) NOT NULL,
    origin_city                   VARCHAR(150) NOT NULL,

    destination_province          VARCHAR(100) NOT NULL,
    destination_city              VARCHAR(150) NOT NULL,

    service_type                  VARCHAR(50)  NOT NULL DEFAULT 'EZ',
    bag_size                      VARCHAR(50)  NOT NULL DEFAULT 'Small (<=3KG)',

    min_weight_kg                 DECIMAL(10,2) NOT NULL DEFAULT 0,
    max_weight_kg                 DECIMAL(10,2) NULL,

    shipping_fee                  DECIMAL(10,2) NOT NULL,
    item_additional_fee           DECIMAL(10,2) NOT NULL DEFAULT 0,
    additional_fee_per_kg_over_max DECIMAL(10,2) NOT NULL DEFAULT 0,

    active                        BOOLEAN NOT NULL DEFAULT TRUE,

    created_at                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_jnt_rates_route
    ON jnt_shipping_rates (origin_province, origin_city, destination_province, destination_city);

CREATE INDEX idx_jnt_rates_destination
    ON jnt_shipping_rates (destination_province, destination_city);