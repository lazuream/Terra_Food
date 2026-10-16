CREATE TABLE food_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(30) NOT NULL,
    normalized_name VARCHAR(30) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_by BIGINT NULL,
    merged_into_id BIGINT NULL,
    version INT NOT NULL DEFAULT 0,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_food_tag_type_name (type, normalized_name),
    INDEX idx_food_tag_status_type (status, type),
    CONSTRAINT fk_food_tag_user FOREIGN KEY (created_by) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_food_tag_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user(id) ON DELETE SET NULL,
    CONSTRAINT fk_food_tag_merged FOREIGN KEY (merged_into_id) REFERENCES food_tag(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_tag_alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_id BIGINT NOT NULL,
    alias VARCHAR(30) NOT NULL,
    normalized_alias VARCHAR(30) NOT NULL,
    UNIQUE KEY uk_food_tag_alias (normalized_alias),
    CONSTRAINT fk_food_tag_alias_tag FOREIGN KEY(tag_id) REFERENCES food_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_tag_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    action VARCHAR(24) NOT NULL,
    detail VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_food_tag_audit_tag (tag_id, created_at),
    CONSTRAINT fk_food_tag_audit_tag FOREIGN KEY(tag_id) REFERENCES food_tag(id) ON DELETE CASCADE,
    CONSTRAINT fk_food_tag_audit_actor FOREIGN KEY(actor_id) REFERENCES app_user(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_alias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    food_id BIGINT NOT NULL,
    alias VARCHAR(100) NOT NULL,
    normalized_alias VARCHAR(100) NOT NULL,
    UNIQUE KEY uk_food_alias_food_name (food_id, normalized_alias),
    INDEX idx_food_alias_name (normalized_alias),
    CONSTRAINT fk_food_alias_food FOREIGN KEY(food_id) REFERENCES food(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_tag_link (
    food_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY(food_id, tag_id),
    CONSTRAINT fk_food_tag_link_food FOREIGN KEY(food_id) REFERENCES food(id) ON DELETE CASCADE,
    CONSTRAINT fk_food_tag_link_tag FOREIGN KEY(tag_id) REFERENCES food_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
