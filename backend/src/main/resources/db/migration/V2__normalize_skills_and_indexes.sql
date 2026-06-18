-- ============================================================
-- V2: Normalize Skills & Add Search Indexes
-- 1. Creates normalized job_skills tables (replacing plain TEXT column)
-- 2. Adds FULLTEXT indexes for search on jobs
-- 3. Adds composite indexes for common query patterns
-- ============================================================

-- ============================================================
-- 1. SKILL NORMALIZATION
-- Create normalized tables to replace denormalized required_skills TEXT
-- ============================================================

-- Skills lookup table (normalized skill names)
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_skill_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Job-Skills junction table (many-to-many)
CREATE TABLE IF NOT EXISTS job_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    CONSTRAINT fk_job_skill_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE KEY uq_job_skill (job_id, skill_id),
    INDEX idx_job_skills_job (job_id),
    INDEX idx_job_skills_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. FULL-TEXT SEARCH INDEXES
-- Enables efficient text-based searching on job listings
-- ============================================================

-- Safely add FULLTEXT index (check if it exists first)
SET @ft_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
                  WHERE table_schema = DATABASE() AND table_name = 'jobs' AND index_name = 'ft_job_search');
SET @ft_sql = IF(@ft_exists = 0,
    'ALTER TABLE jobs ADD FULLTEXT INDEX ft_job_search (title, description, company, location)',
    'SELECT 1');
PREPARE stmt_ft FROM @ft_sql;
EXECUTE stmt_ft;
DEALLOCATE PREPARE stmt_ft;

-- ============================================================
-- 3. COMPOSITE INDEXES
-- Optimize common query patterns WITHOUT dropping existing indexes.
-- The old single-column indexes are harmless and provide safety
-- during transition. New composites improve performance for
-- multi-column queries.
-- ============================================================

-- Composite: recruiter_id + status (recruiter's dashboard filtered by status)
SET @idx_recruiter_status_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'jobs' AND index_name = 'idx_job_recruiter_status');
SET @sql_recruiter_status = IF(@idx_recruiter_status_exists = 0,
    'CREATE INDEX idx_job_recruiter_status ON jobs (recruiter_id, status)',
    'SELECT 1');
PREPARE stmt_rs FROM @sql_recruiter_status;
EXECUTE stmt_rs;
DEALLOCATE PREPARE stmt_rs;

-- Composite: status + is_deleted + created_at (active jobs sorted by date)
SET @idx_active_listing_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'jobs' AND index_name = 'idx_job_active_listing');
SET @sql_active_listing = IF(@idx_active_listing_exists = 0,
    'CREATE INDEX idx_job_active_listing ON jobs (status, is_deleted, created_at)',
    'SELECT 1');
PREPARE stmt_al FROM @sql_active_listing;
EXECUTE stmt_al;
DEALLOCATE PREPARE stmt_al;

-- Composite: status + location (filtered search)
SET @idx_status_location_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'jobs' AND index_name = 'idx_job_status_location');
SET @sql_status_location = IF(@idx_status_location_exists = 0,
    'CREATE INDEX idx_job_status_location ON jobs (status, location)',
    'SELECT 1');
PREPARE stmt_sl FROM @sql_status_location;
EXECUTE stmt_sl;
DEALLOCATE PREPARE stmt_sl;

-- Composite for applications: applicant_id + status
SET @idx_applicant_status_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'applications' AND index_name = 'idx_app_applicant_status');
SET @sql_applicant_status = IF(@idx_applicant_status_exists = 0,
    'CREATE INDEX idx_app_applicant_status ON applications (applicant_id, status)',
    'SELECT 1');
PREPARE stmt_aps FROM @sql_applicant_status;
EXECUTE stmt_aps;
DEALLOCATE PREPARE stmt_aps;

-- Composite for applications: job_id + status
SET @idx_job_status_app_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'applications' AND index_name = 'idx_app_job_status');
SET @sql_job_status_app = IF(@idx_job_status_app_exists = 0,
    'CREATE INDEX idx_app_job_status ON applications (job_id, status)',
    'SELECT 1');
PREPARE stmt_jsa FROM @sql_job_status_app;
EXECUTE stmt_jsa;
DEALLOCATE PREPARE stmt_jsa;

-- Composite for applications: applicant_id + is_deleted
SET @idx_applicant_deleted_exists = (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'applications' AND index_name = 'idx_app_applicant_deleted');
SET @sql_applicant_deleted = IF(@idx_applicant_deleted_exists = 0,
    'CREATE INDEX idx_app_applicant_deleted ON applications (applicant_id, is_deleted)',
    'SELECT 1');
PREPARE stmt_apd FROM @sql_applicant_deleted;
EXECUTE stmt_apd;
DEALLOCATE PREPARE stmt_apd;