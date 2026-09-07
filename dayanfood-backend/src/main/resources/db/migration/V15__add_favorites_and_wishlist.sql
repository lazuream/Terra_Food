CREATE TABLE food_favorite (
    user_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, food_id),
    CONSTRAINT fk_food_favorite_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_food_favorite_food FOREIGN KEY (food_id) REFERENCES food (id) ON DELETE CASCADE,
    INDEX idx_food_favorite_food (food_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wishlist_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content VARCHAR(100) NOT NULL,
    normalized_content VARCHAR(100) NOT NULL,
    source_food_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_item_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_item_food FOREIGN KEY (source_food_id) REFERENCES food (id) ON DELETE SET NULL,
    CONSTRAINT uk_wishlist_item_content UNIQUE (user_id, normalized_content),
    INDEX idx_wishlist_item_user_created (user_id, created_at),
    INDEX idx_wishlist_item_source_food (source_food_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
