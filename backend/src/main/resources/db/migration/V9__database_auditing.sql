-- ============================================================
-- V9: Database Auditing - Change tracking, history, rollback
-- ============================================================

-- Database change history table
CREATE TABLE IF NOT EXISTS schema_changes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    author VARCHAR(255),
    script_name VARCHAR(500),
    checksum VARCHAR(64),
    installed_by VARCHAR(255) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time_ms BIGINT NOT NULL DEFAULT 0,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_schema_version (version),
    INDEX idx_schema_installed (installed_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Data change audit for sensitive tables
CREATE TABLE IF NOT EXISTS data_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    action ENUM('INSERT', 'UPDATE', 'DELETE') NOT NULL,
    old_data JSON,
    new_data JSON,
    changed_by BIGINT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transaction_id VARCHAR(36),
    ip_address VARCHAR(45),
    INDEX idx_audit_table (table_name, record_id),
    INDEX idx_audit_time (changed_at),
    INDEX idx_audit_user (changed_by),
    INDEX idx_audit_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table to track reversible migrations for rollback
CREATE TABLE IF NOT EXISTS migration_rollback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    migration_version VARCHAR(50) NOT NULL,
    rollback_script TEXT NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPLIED', 'FAILED') DEFAULT 'PENDING',
    checksum VARCHAR(64),
    UNIQUE KEY uk_rollback_version (migration_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert initial rollback records for reversible migrations
INSERT IGNORE INTO migration_rollback (migration_version, rollback_script, status) VALUES
('V9', 'DROP TABLE IF EXISTS data_audit_log; DROP TABLE IF EXISTS schema_changes; DROP TABLE IF EXISTS migration_rollback;', 'PENDING');

-- Performance statistics table for benchmarking
CREATE TABLE IF NOT EXISTS performance_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    response_time_ms INT NOT NULL,
    status_code INT NOT NULL,
    user_id BIGINT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    query_count INT DEFAULT 0,
    query_time_ms INT DEFAULT 0,
    INDEX idx_perf_endpoint (endpoint),
    INDEX idx_perf_time (recorded_at),
    INDEX idx_perf_slow (response_time_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Slow query log for database monitoring
CREATE TABLE IF NOT EXISTS slow_query_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_text TEXT NOT NULL,
    database_name VARCHAR(100),
    execution_time_ms INT NOT NULL,
    rows_examined BIGINT DEFAULT 0,
    rows_sent BIGINT DEFAULT 0,
    lock_time_ms INT DEFAULT 0,
    thread_id BIGINT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_slow_time (recorded_at),
    INDEX idx_slow_duration (execution_time_ms),
    INDEX idx_slow_table (database_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Configuration for migration rollback
INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('audit_log_retention_days', '365', 'Number of days to retain audit logs'),
('slow_query_threshold_ms', '500', 'Threshold in MS for slow query logging'),
('perf_stats_retention_days', '90', 'Days to retain performance statistics'),
('backup_enabled', 'true', 'Enable automated daily backups'),
('backup_retention_days', '30', 'Number of days to retain backups'),
('backup_time', '02:00', 'Scheduled backup time (UTC)'),
('read_replica_enabled', 'false', 'Enable read replicas for query scaling'),
('reversible_migrations', 'true', 'Enable reversible migration tracking');