-- ============================================================
-- V6: Convert required_skills column to JSON format
-- Migrates existing TEXT data to JSON array format
-- ============================================================

-- Step 1: Alter the column type to JSON
ALTER TABLE jobs MODIFY COLUMN required_skills JSON DEFAULT '[]';