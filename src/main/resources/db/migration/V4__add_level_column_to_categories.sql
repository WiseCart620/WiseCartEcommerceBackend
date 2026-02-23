ALTER TABLE categories ADD COLUMN level INT NOT NULL DEFAULT 0;

-- Update existing categories with correct level values
UPDATE categories SET level = 0 WHERE parent_id IS NULL;

-- For categories with parents, we need to update recursively
-- This is a simplified approach - you might need a stored procedure for complex hierarchies
UPDATE categories c 
JOIN categories p ON c.parent_id = p.id 
SET c.level = p.level + 1 
WHERE c.parent_id IS NOT NULL;