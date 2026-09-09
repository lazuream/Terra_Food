CREATE TABLE food_create_idempotency (
    user_id BIGINT NOT NULL,
    idem_key VARCHAR(100) NOT NULL,
    food_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, idem_key),
    CONSTRAINT fk_food_idem_user FOREIGN KEY(user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_food_idem_food FOREIGN KEY(food_id) REFERENCES food(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
