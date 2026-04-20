-- =====================================================
-- Flyway Migration: V61__add_maya_transaction_reference_columns.sql
-- Description: Add maya_transaction_reference columns to payments and pending_checkouts
-- =====================================================

-- Add column to payments table
ALTER TABLE payments 
ADD COLUMN maya_transaction_reference VARCHAR(255) DEFAULT NULL;

-- Add index to payments table
CREATE INDEX idx_payments_maya_transaction_ref ON payments(maya_transaction_reference);

-- Add column to pending_checkouts table
ALTER TABLE pending_checkouts 
ADD COLUMN maya_transaction_reference VARCHAR(255) DEFAULT NULL;

-- Add index to pending_checkouts table
CREATE INDEX idx_pending_checkouts_maya_transaction_ref ON pending_checkouts(maya_transaction_reference);