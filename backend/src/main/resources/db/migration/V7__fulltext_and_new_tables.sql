-- ============================================================
-- V7: Fulltext Indexes, Audit Log, Notifications, Reports, Batch Tables
-- ============================================================

-- FULLTEXT index for jobs table (title, description, company, location)
ALTER TABLE jobs ADD FULLTEXT INDEX ft_jobs_search (title, description, company, location);

-- Audit Log table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_user_action (user_id, action),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- WebSocket Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_created (created_at),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Weekly Statistics table
CREATE TABLE IF NOT EXISTS weekly_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    new_jobs INT NOT NULL DEFAULT 0,
    new_applications INT NOT NULL DEFAULT 0,
    new_users INT NOT NULL DEFAULT 0,
    new_recruiters INT NOT NULL DEFAULT 0,
    active_jobs INT NOT NULL DEFAULT 0,
    total_applications INT NOT NULL DEFAULT 0,
    hired_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_weekly_week_start (week_start),
    INDEX idx_weekly_week (week_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Monthly Reports table
CREATE TABLE IF NOT EXISTS monthly_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    month_year VARCHAR(7) NOT NULL,
    total_jobs INT NOT NULL DEFAULT 0,
    total_applications INT NOT NULL DEFAULT 0,
    total_users INT NOT NULL DEFAULT 0,
    total_recruiters INT NOT NULL DEFAULT 0,
    applications_per_job DECIMAL(10,2) NOT NULL DEFAULT 0,
    hiring_conversion_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
    top_skills TEXT,
    recruiter_performance TEXT,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_monthly_month_year (month_year),
    INDEX idx_monthly_month (month_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- System Settings table
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description VARCHAR(500),
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_settings_key (setting_key),
    CONSTRAINT fk_settings_user FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Batch Job Tracking table
CREATE TABLE IF NOT EXISTS batch_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_items INT NOT NULL DEFAULT 0,
    processed_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    error_message TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_status (status),
    INDEX idx_batch_type (job_type),
    INDEX idx_batch_created (created_at),
    CONSTRAINT fk_batch_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default system settings
INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('job_expiry_days', '30', 'Number of days after which active jobs expire'),
('token_cleanup_days', '7', 'Number of days to keep expired verification tokens'),
('weekly_stats_enabled', 'true', 'Enable weekly statistics generation'),
('monthly_reports_enabled', 'true', 'Enable monthly report generation'),
('max_bulk_upload_size', '100', 'Maximum number of resumes for bulk upload'),
('email_notifications_enabled', 'true', 'Enable email notifications'),
('default_page_size', '20', 'Default pagination page size');