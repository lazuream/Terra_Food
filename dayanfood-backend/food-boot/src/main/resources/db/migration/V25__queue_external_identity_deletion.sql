CREATE TABLE external_identity_deletion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject_id CHAR(36) NOT NULL,
    target VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error_code VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uk_external_delete_subject_target (subject_id, target),
    KEY idx_external_delete_pending (status, next_attempt_at)
);
