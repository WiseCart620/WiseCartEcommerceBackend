-- V84__expand_payment_status_enum.sql
-- Expands payment_status ENUM on payments.status and orders.payment_status
-- to include UNPAID, PROCESSING_REFUND, PARTIALLY_REFUNDED, CANCELLED
-- Matches current definition: nullable, default 'PENDING'

ALTER TABLE payments
    MODIFY COLUMN status ENUM(
        'PENDING',
        'UNPAID',
        'COMPLETED',
        'PROCESSING_REFUND',
        'PARTIALLY_REFUNDED',
        'REFUNDED',
        'FAILED',
        'CANCELLED'
    ) NULL DEFAULT 'PENDING';

ALTER TABLE orders
    MODIFY COLUMN payment_status ENUM(
        'PENDING',
        'UNPAID',
        'COMPLETED',
        'PROCESSING_REFUND',
        'PARTIALLY_REFUNDED',
        'REFUNDED',
        'FAILED',
        'CANCELLED'
    ) NULL DEFAULT 'PENDING';