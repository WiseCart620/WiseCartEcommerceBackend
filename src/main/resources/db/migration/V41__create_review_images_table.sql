-- V41__create_review_images_table.sql
-- Creates the review_images table to store customer-uploaded photos per review.
-- Linked to reviews with CASCADE DELETE so images are removed when a review is deleted.

CREATE TABLE IF NOT EXISTS review_images (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    review_id     BIGINT          NOT NULL,
    image_url     VARCHAR(500)    NOT NULL,
    display_order INT             NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT fk_review_images_review
        FOREIGN KEY (review_id)
        REFERENCES reviews (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_review_images_review_id (review_id),
    INDEX idx_review_images_display_order (review_id, display_order)
);