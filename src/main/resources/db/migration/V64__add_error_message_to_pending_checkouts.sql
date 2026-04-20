-- Add error_message column to pending_checkouts table
ALTER TABLE pending_checkouts 
ADD COLUMN error_message VARCHAR(500) NULL;