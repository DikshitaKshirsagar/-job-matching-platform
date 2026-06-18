-- ============================================================
-- V1: Initial Schema (Baseline)
-- Creates all core tables if they do not already exist.
-- This replaces the old V1__init_schema.sql and V2__saved_jobs_and_profile_updates.sql
-- from the initial commit, providing a single consolidated baseline.
-- ============================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ROLE_JOB_SEEKER','ROLE_RECRUITER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_JOB_SEEKER',
    resume_text LONGTEXT,
    resume_file_name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expiry DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry DATETIME,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role),
    INDEX idx_user_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Jobs table
CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description LONGTEXT NOT NULL,
    company VARCHAR(150) NOT NULL,
    location VARCHAR(150) NOT NULL,
    job_type ENUM('FULL_TIME','PART_TIME','CONTRACT','INTERNSHIP','REMOTE','HYBRID') DEFAULT 'FULL_TIME',
    status ENUM('ACTIVE','CLOSED','DRAFT','EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    salary_min DECIMAL(12,2),
    salary_max DECIMAL(12,2),
    required_skills LONGTEXT,
    recruiter_id BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id),
    INDEX idx_job_status (status),
    INDEX idx_job_recruiter (recruiter_id),
    INDEX idx_job_location (location),
    INDEX idx_job_created_at (created_at),
    INDEX idx_job_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Applications table
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

-- Saved Jobs table
CREATE TABLE IF NOT EXISTS saved_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_job_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_saved_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    UNIQUE KEY uk_saved_job_user_job (user_id, job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;