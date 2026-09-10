CREATE TABLE image_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_url VARCHAR(512) NOT NULL,
    original_width INT NOT NULL,
    original_height INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    variant_320_url VARCHAR(512) NULL,
    variant_640_url VARCHAR(512) NULL,
    variant_1280_url VARCHAR(512) NULL,
    processor_version INT NOT NULL DEFAULT 1,
    error_code VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_image_asset_original (original_url),
    INDEX idx_image_asset_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE food
    ADD INDEX idx_food_review_heat_id (review_status, heat DESC, id DESC),
    ADD INDEX idx_food_review_region_heat_id (review_status, region_id, heat DESC, id DESC),
    ADD INDEX idx_food_review_created_id (review_status, created_at DESC, id DESC),
    ADD INDEX idx_food_image_url (image_url);

ALTER TABLE food_comment
    ADD INDEX idx_food_comment_food_created_id (food_id, created_at DESC, id DESC);
