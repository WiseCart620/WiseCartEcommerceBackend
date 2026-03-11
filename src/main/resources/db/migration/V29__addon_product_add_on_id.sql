-- V29: Add missing addon_product_add_on_id column to cart_items and order_items
-- V28 created addon_add_on_id but the entity uses addon_product_add_on_id

-- ── cart_items ────────────────────────────────────────────────────────────────

ALTER TABLE cart_items
    ADD COLUMN addon_product_add_on_id BIGINT NULL;

ALTER TABLE cart_items
    ADD CONSTRAINT fk_cart_items_addon_product_add_on
        FOREIGN KEY (addon_product_add_on_id)
        REFERENCES product_add_ons(id)
        ON DELETE SET NULL;

-- ── order_items ───────────────────────────────────────────────────────────────

ALTER TABLE order_items
    ADD COLUMN addon_product_add_on_id BIGINT NULL;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_addon_product_add_on
        FOREIGN KEY (addon_product_add_on_id)
        REFERENCES product_add_ons(id)
        ON DELETE SET NULL;