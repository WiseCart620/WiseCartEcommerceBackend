-- Fix any existing lowercase values
UPDATE pending_checkouts SET status = 'PENDING'   WHERE status = 'pending';
UPDATE pending_checkouts SET status = 'COMPLETED' WHERE status = 'completed';
UPDATE pending_checkouts SET status = 'FAILED'    WHERE status = 'failed';
UPDATE pending_checkouts SET status = 'EXPIRED'   WHERE status = 'expired';

-- Update the column definition to use uppercase enum values
ALTER TABLE pending_checkouts
  MODIFY COLUMN status ENUM('PENDING','COMPLETED','FAILED','EXPIRED') NOT NULL DEFAULT 'PENDING';