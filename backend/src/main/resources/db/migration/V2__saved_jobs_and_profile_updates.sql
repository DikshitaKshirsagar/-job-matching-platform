CREATE TABLE IF NOT EXISTS saved_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_saved_job_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_saved_job_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT uk_saved_job_user_job UNIQUE (user_id, job_id)
);
