CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    resume_text TEXT,
    resume_file_name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(255),
    verification_token_expiry DATETIME,
    password_reset_token VARCHAR(255),
    password_reset_token_expiry DATETIME,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    skills TEXT,
    location VARCHAR(255),
    salary VARCHAR(255),
    recruiter_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_job_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    job_id BIGINT,
    match_score DOUBLE,
    status VARCHAR(50),
    applied_at DATETIME,
    CONSTRAINT fk_application_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);
