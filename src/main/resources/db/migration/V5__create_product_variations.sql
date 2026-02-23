-- ─────────────────────────────────────────────────────────────────────────────
-- Product Variations
-- Each variation belongs to a product and has its own price, sku, upc,
-- stock quantity, discount, and optional image.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE product_variations (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    product_id       BIGINT         NOT NULL,

    name             VARCHAR(255)   NOT NULL,
    sku              VARCHAR(100)   NULL,
    upc              VARCHAR(100)   NULL,

    price            DECIMAL(10, 2) NOT NULL,
    discount         DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,

    stock_quantity   INT            NOT NULL DEFAULT 0,
    image_url        VARCHAR(500)   NULL,

    active           BOOLEAN        NOT NULL DEFAULT TRUE,

    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    CONSTRAINT fk_variation_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE CASCADE,

    -- SKU and UPC must be globally unique across all variations
    CONSTRAINT uq_variation_sku UNIQUE (sku),
    CONSTRAINT uq_variation_upc UNIQUE (upc),

    INDEX idx_variation_product  (product_id),
    INDEX idx_variation_sku      (sku),
    INDEX idx_variation_active   (active)
);