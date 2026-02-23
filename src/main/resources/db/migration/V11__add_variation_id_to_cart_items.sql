-- Remove existing cart items so old items without variation_id don't cause stale data issues
DELETE FROM cart_items;
