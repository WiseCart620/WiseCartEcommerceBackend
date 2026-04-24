CREATE TABLE IF NOT EXISTS badge_colors (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    badge_name VARCHAR(100) NOT NULL UNIQUE,
    color_class VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO badge_colors (badge_name, color_class, active, display_order) VALUES
('New', 'bg-orange-500', TRUE, 1),
('Hot', 'bg-orange-500', TRUE, 2),
('Sale', 'bg-orange-500', TRUE, 3),
('Trending', 'bg-orange-500', TRUE, 4),
('Limited', 'bg-orange-500', TRUE, 5),
('Staff Pick', 'bg-orange-500', TRUE, 6),
('Exclusive', 'bg-orange-500', TRUE, 7),
('Best Value', 'bg-orange-500', TRUE, 8);