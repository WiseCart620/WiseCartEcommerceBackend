-- Add verification_token column to pending_checkouts table
ALTER TABLE pending_checkouts 
ADD COLUMN verification_token VARCHAR(255) NULL;

-- Add index for faster lookups
CREATE INDEX idx_pending_checkouts_verification_token ON pending_checkouts(verification_token);