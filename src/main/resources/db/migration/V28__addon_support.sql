-- V28: Add add-on support to cart_items and order_items
-- Add-ons are stored as separate line items linked back to their ProductAddOn config row

-- ── cart_items ────────────────────────────────────────────────────────────────

ALTER TABLE cart_items
    ADD COLUMN is_addon           BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN addon_product_id   BIGINT         NULL,
    ADD COLUMN addon_add_on_id    BIGINT         NULL,
    ADD COLUMN addon_variation_id BIGINT         NULL,
    ADD COLUMN addon_price        DECIMAL(10,2)  NULL;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_addon_product
        FOREIGN KEY (addon_product_id)
        REFERENCES products(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_cart_items_addon_add_on
        FOREIGN KEY (addon_add_on_id)
        REFERENCES product_add_ons(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_cart_items_addon_variation
        FOREIGN KEY (addon_variation_id)
        REFERENCES product_variations(id)
        ON DELETE SET NULL;

-- ── order_items ───────────────────────────────────────────────────────────────

ALTER TABLE order_items
    ADD COLUMN is_addon           BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN addon_product_id   BIGINT         NULL,
    ADD COLUMN addon_add_on_id    BIGINT         NULL,
    ADD COLUMN addon_variation_id BIGINT         NULL,
    ADD COLUMN addon_price        DECIMAL(10,2)  NULL;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_addon_product
        FOREIGN KEY (addon_product_id)
        REFERENCES products(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_order_items_addon_add_on
        FOREIGN KEY (addon_add_on_id)
        REFERENCES product_add_ons(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_order_items_addon_variation
        FOREIGN KEY (addon_variation_id)
        REFERENCES product_variations(id)
        ON DELETE SET NULL;