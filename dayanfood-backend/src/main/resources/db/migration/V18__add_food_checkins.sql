CREATE TABLE food_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    food_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    food_name_snapshot VARCHAR(100) NOT NULL,
    eaten_on DATE NOT NULL,
    note VARCHAR(500) NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_food_checkin_food FOREIGN KEY (food_id) REFERENCES food(id) ON DELETE SET NULL,
    CONSTRAINT fk_food_checkin_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    INDEX idx_food_checkin_user_date (user_id, eaten_on, id),
    INDEX idx_food_checkin_food_public (food_id, visibility, eaten_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
