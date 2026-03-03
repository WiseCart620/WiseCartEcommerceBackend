-- ─────────────────────────────────────────────────────────────────────────────
-- V22__add_hero_banners_and_testimonials.sql
-- Adds hero_banners and testimonials tables for homepage content management.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── hero_banners ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hero_banners (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    title            VARCHAR(100)    NOT NULL,
    badge            VARCHAR(100)    NULL,
    subtitle         VARCHAR(200)    NULL,
    button_text      VARCHAR(50)     NOT NULL DEFAULT 'Shop Now',
    button_link      VARCHAR(255)    NOT NULL DEFAULT '/products',
    image_url        TEXT            NULL,
    text_color       VARCHAR(10)     NOT NULL DEFAULT 'light'
                                     COMMENT 'light or dark — controls text colour over the image',
    overlay_opacity  INT             NOT NULL DEFAULT 40
                                     COMMENT '0–100 darkness percentage of the black overlay',
    display_order    INT             NOT NULL DEFAULT 0,
    active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_hero_banners PRIMARY KEY (id)
);

CREATE INDEX idx_hero_banners_active_order
    ON hero_banners (active, display_order);

-- ── testimonials ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS testimonials (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    customer_name    VARCHAR(100)    NOT NULL,
    customer_title   VARCHAR(100)    NULL
                                     COMMENT 'e.g. Verified Buyer, Fashion Blogger',
    avatar_url       TEXT            NULL,
    review           TEXT            NOT NULL,
    rating           INT             NOT NULL DEFAULT 5
                                     COMMENT '1–5 star rating',
    product_name     VARCHAR(150)    NULL
                                     COMMENT 'Optional — which product they reviewed',
    display_order    INT             NOT NULL DEFAULT 0,
    active           TINYINT(1)      NOT NULL DEFAULT 1,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_testimonials        PRIMARY KEY (id),
    CONSTRAINT chk_testimonials_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_testimonials_active_order
    ON testimonials (active, display_order);
