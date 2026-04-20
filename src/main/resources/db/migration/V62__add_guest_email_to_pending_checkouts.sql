-- Add guest_email column to pending_checkouts table
ALTER TABLE pending_checkouts 
ADD COLUMN guest_email VARCHAR(255) NULL;

-- Add index for guest_email lookups
CREATE INDEX idx_pending_checkouts_guest_email ON pending_checkouts(guest_email);