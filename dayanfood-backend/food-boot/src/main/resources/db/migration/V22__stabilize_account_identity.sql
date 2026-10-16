ALTER TABLE app_user
    ADD COLUMN subject_id CHAR(36) NULL AFTER id,
    ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 1 AFTER active;

UPDATE app_user SET subject_id = UUID() WHERE subject_id IS NULL;

ALTER TABLE app_user
    MODIFY subject_id CHAR(36) NOT NULL,
    ADD CONSTRAINT uk_app_user_subject UNIQUE (subject_id);

ALTER TABLE food
    ADD COLUMN created_by_user_id BIGINT NULL AFTER created_by,
    ADD INDEX idx_food_created_by_user (created_by_user_id, created_at, id);

-- Only currently existing, unique usernames can be linked with confidence. Imported
-- display names and deleted accounts intentionally remain unowned.
UPDATE food f
INNER JOIN app_user u ON u.username = f.created_by
SET f.created_by_user_id = u.id
WHERE f.created_by_user_id IS NULL;

ALTER TABLE food
    ADD CONSTRAINT fk_food_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL;
