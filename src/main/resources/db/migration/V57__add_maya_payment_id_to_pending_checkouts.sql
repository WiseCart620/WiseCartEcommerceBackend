-- Flyway Migration V57
-- Add maya_payment_id column to pending_checkouts table

ALTER TABLE pending_checkouts
    ADD COLUMN maya_payment_id VARCHAR(255) NULL;