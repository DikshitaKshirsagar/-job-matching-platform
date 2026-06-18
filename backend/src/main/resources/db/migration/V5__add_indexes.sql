-- ============================================================
-- V5: Add Performance Indexes
-- - Composite unique index on applications(applicant_id, job_id)
--   to enforce "apply once" at DB level and speed up
--   existsByApplicantIdAndJobId() queries.
-- - Fulltext index on jobs(title, description, company)
--   for fast text-based job search.
-- - Index on applications(status) for filtering by status.
-- ============================================================

CREATE UNIQUE INDEX idx_app_applicant_job
ON applications(applicant_id, job_id);

CREATE FULLTEXT INDEX idx_job_search
ON jobs(title, description, company);

CREATE INDEX idx_app_status
ON applications(status);