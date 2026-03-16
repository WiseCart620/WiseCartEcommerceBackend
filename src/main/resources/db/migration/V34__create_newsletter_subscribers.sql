-- V{next}__create_newsletter_subscribers.sql
CREATE TABLE newsletter_subscribers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    source        VARCHAR(50)  NOT NULL DEFAULT 'WEBSITE',
    subscribed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active        TINYINT(1)   NOT NULL DEFAULT 1
);