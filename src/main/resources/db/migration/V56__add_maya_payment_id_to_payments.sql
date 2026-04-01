-- Flyway Migration V56
-- Add maya_payment_id column to payments table

ALTER TABLE payments
    ADD COLUMN maya_payment_id VARCHAR(255) NULL;