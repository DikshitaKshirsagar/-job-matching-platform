CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    applicant_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    status ENUM('PENDING','UNDER_REVIEW','SHORTLISTED','REJECTED','HIRED') NOT NULL DEFAULT 'PENDING',
    cover_letter LONGTEXT,
    match_score DOUBLE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_applicant FOREIGN KEY (applicant_id) REFERENCES users(id),
    CONSTRAINT fk_app_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    UNIQUE KEY uq_applicant_job (applicant_id, job_id),
    INDEX idx_app_applicant (applicant_id),
    INDEX idx_app_job (job_id),
    INDEX idx_app_status (status),
    INDEX idx_app_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
