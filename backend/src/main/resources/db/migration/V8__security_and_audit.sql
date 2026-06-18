-- ============================================================
-- V8: Security & Audit Enhancements
-- Account lockout, audit logging, GDPR compliance
-- ============================================================

-- Account lockout table
CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    ip_address VARCHAR(45) NOT NULL,
    email VARCHAR(255) NOT NULL,
    attempt_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_login_email (email),
    INDEX idx_login_ip (ip_address),
    INDEX idx_login_time (attempt_time),
    INDEX idx_login_user (user_id),
    CONSTRAINT fk_login_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Account lockout tracking
CREATE TABLE IF NOT EXISTS account_lockout (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lockout_email (email),
    INDEX idx_lockout_user (user_id),
    INDEX idx_lockout_locked (locked_until),
    CONSTRAINT fk_lockout_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Enhanced audit log for security events
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_type VARCHAR(50) AFTER entity_type;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS resource_id VARCHAR(255) AFTER resource_type;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS old_value TEXT AFTER details;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS new_value TEXT AFTER old_value;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(36) AFTER new_value;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS session_id VARCHAR(100) AFTER correlation_id;
ALTER TABLE audit_logs ADD INDEX IF NOT EXISTS idx_audit_resource (resource_type, resource_id);
ALTER TABLE audit_logs ADD INDEX IF NOT EXISTS idx_audit_correlation (correlation_id);

-- GDPR data deletion requests
CREATE TABLE IF NOT EXISTS gdpr_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_type ENUM('DELETE', 'EXPORT') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    data_export_path VARCHAR(500),
    notes TEXT,
    INDEX idx_gdpr_user (user_id),
    INDEX idx_gdpr_status (status),
    CONSTRAINT fk_gdpr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit configuration settings
INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('account_lockout_threshold', '5', 'Number of failed login attempts before account lockout'),
('account_lockout_duration_minutes', '15', 'Duration of account lockout in minutes'),
('password_min_length', '8', 'Minimum password length requirement'),
('password_require_uppercase', 'true', 'Require uppercase letter in password'),
('password_require_lowercase', 'true', 'Require lowercase letter in password'),
('password_require_number', 'true', 'Require number in password'),
('password_require_special', 'true', 'Require special character in password'),
('audit_log_retention_days', '365', 'Number of days to retain audit logs'),
('session_timeout_minutes', '60', 'Session timeout in minutes'),
('max_login_attempts_per_ip', '20', 'Maximum login attempts per IP per hour'),
('gdpr_data_retention_days', '30', 'Days to fulfill GDPR data requests'),
('encryption_enabled', 'true', 'Enable field-level encryption for PII'),
('security_headers_enabled', 'true', 'Enable security headers globally');