CREATE TABLE IF NOT EXISTS saved_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_job_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_saved_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    UNIQUE KEY uq_saved_job (user_id, job_id),
    INDEX idx_saved_job_user (user_id),
    INDEX idx_saved_job_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
