-- V80__add_product_slug.sql

-- ── Products: slug column for SEO-friendly URLs ──
ALTER TABLE products
    ADD COLUMN slug VARCHAR(255);

-- Backfill existing rows from name (same pattern used for categories)
UPDATE products
SET slug = LOWER(REGEXP_REPLACE(TRIM(name), '[^a-zA-Z0-9]+', '-'))
WHERE slug IS NULL;

-- De-duplicate any collisions the naive backfill produced, by
-- appending the product id to every row after the first per slug.
UPDATE products p
JOIN (
    SELECT id, slug,
           ROW_NUMBER() OVER (PARTITION BY slug ORDER BY id) AS rn
    FROM products
) ranked ON p.id = ranked.id
SET p.slug = CONCAT(p.slug, '-', p.id)
WHERE ranked.rn > 1;

-- Now safe to enforce uniqueness at the DB level, matching the entity's
-- unique = true on the slug column.
ALTER TABLE products
    ADD CONSTRAINT uq_products_slug UNIQUE (slug);