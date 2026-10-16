CREATE TABLE food_checkin_idempotency (
    user_id BIGINT NOT NULL,
    idem_key VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    checkin_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    PRIMARY KEY (user_id, idem_key),
    CONSTRAINT fk_checkin_idem_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_checkin_idem_checkin FOREIGN KEY (checkin_id) REFERENCES food_checkin(id) ON DELETE CASCADE,
    INDEX idx_checkin_idem_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
