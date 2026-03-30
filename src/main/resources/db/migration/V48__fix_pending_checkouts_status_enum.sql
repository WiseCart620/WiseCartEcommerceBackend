ALTER TABLE pending_checkouts
  MODIFY COLUMN status ENUM('pending','completed','failed','expired') NOT NULL DEFAULT 'pending';