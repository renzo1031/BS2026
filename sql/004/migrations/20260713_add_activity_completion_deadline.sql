ALTER TABLE buddy_activity
    ADD COLUMN completion_deadline_at DATETIME(3) NULL AFTER claim_expires_at,
    ADD KEY idx_activity_completion (lifecycle_status, completion_deadline_at, created_at);
