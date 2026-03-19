-- V42__add_created_at_to_review_images.sql
-- Hibernate schema validation requires created_at on review_images.
-- V41 already ran without it, so we add it here as an ALTER.

ALTER TABLE review_images
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);