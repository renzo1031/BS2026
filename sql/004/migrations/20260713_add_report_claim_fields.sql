ALTER TABLE report_case
    ADD COLUMN claim_expires_at DATETIME(3) NULL AFTER assignee_id,
    ADD COLUMN appeal_reason VARCHAR(1000) NULL AFTER resolution,
    ADD COLUMN appeal_resolution VARCHAR(1000) NULL AFTER appeal_reason,
    ADD COLUMN appealed_at DATETIME(3) NULL AFTER appeal_resolution,
    DROP INDEX idx_report_review,
    ADD KEY idx_report_review (campus_id, status, claim_expires_at, created_at);
