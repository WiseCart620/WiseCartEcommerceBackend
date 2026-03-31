ALTER TABLE orders
    MODIFY COLUMN status ENUM(
        'PENDING',
        'PROCESSING',
        'SHIPPED',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'CANCELLED',
        'REFUNDED',
        'RETURNED',
        'FAILED'
    ) NOT NULL;