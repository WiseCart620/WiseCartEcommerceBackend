-- ============================================================
-- V23__add_promo_cards_table.sql
-- Adds the promo_cards table used by the homepage promo card
-- section (Summer Sale / New Collection / Electronics cards).
-- Mirrors the PromoCard entity exactly.
-- ============================================================

CREATE TABLE promo_cards (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    title          VARCHAR(255)    NOT NULL,
    subtitle       VARCHAR(255),
    description    VARCHAR(500),
    button_text    VARCHAR(100),
    link_url       VARCHAR(500),
    image_url      VARCHAR(1000),
    color          VARCHAR(100),       -- Tailwind gradient string e.g. "from-orange-500 to-red-500"
    display_order  INT             NOT NULL DEFAULT 0,
    active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_promo_cards_active_order (active, display_order)
);

-- ── Seed the 3 default cards so the homepage works immediately ──────────────
-- Images and colour values match the FALLBACK_PROMO_CARDS in HomePage.jsx
-- so admins see something useful on first load even before uploading pictures.

INSERT INTO promo_cards (title, subtitle, description, button_text, link_url, color, display_order, active)
VALUES
    ('Summer Sale',    'Up to 50% off',  'On selected items',       'Shop Now', '/products?sale=true',      'from-orange-500 to-red-500',   0, TRUE),
    ('New Collection', 'Spring 2024',    'Shop the latest trends',  'Shop Now', '/products?new=true',       'from-green-500 to-emerald-500', 1, TRUE),
    ('Electronics',    'Tech Deals',     'Best prices on gadgets',  'Shop Now', '/categories/electronics',  'from-blue-500 to-indigo-500',   2, TRUE);