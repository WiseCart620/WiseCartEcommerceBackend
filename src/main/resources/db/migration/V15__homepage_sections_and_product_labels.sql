-- ============================================================
-- V15__homepage_sections_and_product_labels.sql
-- Adds: product label badge, homepage section config tables
-- ============================================================

-- 1. Product label badge column
ALTER TABLE products
    ADD COLUMN label VARCHAR(30) NULL AFTER upc;

-- 2. Homepage section configuration table
CREATE TABLE homepage_section_configs (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    section_key   VARCHAR(50)  NOT NULL,
    title         VARCHAR(100) NOT NULL,
    subtitle      VARCHAR(200) NULL,
    mode          VARCHAR(20)  NOT NULL DEFAULT 'AUTO',
    category_id   BIGINT       NULL,
    `limit`       INT          NOT NULL DEFAULT 8,
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_hsc_section_key (section_key),
    CONSTRAINT fk_hsc_category
        FOREIGN KEY (category_id)
        REFERENCES categories (id)
        ON DELETE SET NULL
);

-- 3. Homepage section → products join table (for MANUAL mode)
CREATE TABLE homepage_section_products (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    section_id    BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    display_order INT    NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_hsp_section_product (section_id, product_id),
    CONSTRAINT fk_hsp_section
        FOREIGN KEY (section_id)
        REFERENCES homepage_section_configs (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_hsp_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE CASCADE
);

-- 4. Seed the four default homepage sections
INSERT INTO homepage_section_configs
    (section_key,   title,               subtitle,                      mode,   `limit`, active, display_order)
VALUES
    ('FEATURED',     'Featured Products', 'Handpicked just for you',     'AUTO', 8,       1,      0),
    ('HOT_DEALS',    'Hot Deals',         'Limited time discounts',      'AUTO', 8,       1,      1),
    ('NEW_ARRIVALS', 'New Arrivals',      'Fresh from the collection',   'AUTO', 8,       1,      2),
    ('BEST_SELLERS', 'Best Sellers',      'Most popular products',       'AUTO', 8,       1,      3);