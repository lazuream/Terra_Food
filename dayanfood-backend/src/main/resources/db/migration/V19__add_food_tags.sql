CREATE TABLE food_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(30) NOT NULL,
    normalized_name VARCHAR(30) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_food_tag_type_name (type, normalized_name),
    INDEX idx_food_tag_status_type (status, type),
    CONSTRAINT fk_food_tag_user FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_tag_link (
    food_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY(food_id, tag_id),
    CONSTRAINT fk_food_tag_link_food FOREIGN KEY(food_id) REFERENCES food(id) ON DELETE CASCADE,
    CONSTRAINT fk_food_tag_link_tag FOREIGN KEY(tag_id) REFERENCES food_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
