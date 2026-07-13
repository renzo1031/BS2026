SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_block;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS moderation_action;
DROP TABLE IF EXISTS report_case;
DROP TABLE IF EXISTS favorite_activity;
DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS activity_status_log;
DROP TABLE IF EXISTS buddy_member;
DROP TABLE IF EXISTS buddy_application;
DROP TABLE IF EXISTS activity_media;
DROP TABLE IF EXISTS activity_tag_relation;
DROP TABLE IF EXISTS activity_tag;
DROP TABLE IF EXISTS buddy_activity;
DROP TABLE IF EXISTS campus_identity_binding;
DROP TABLE IF EXISTS file_object;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS campus;

CREATE TABLE campus (
    id BIGINT PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    code VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    identity_label VARCHAR(40) NOT NULL DEFAULT '学号',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_campus_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    campus_id BIGINT NULL,
    username VARCHAR(64) NULL,
    password_hash VARCHAR(100) NULL,
    wechat_openid VARCHAR(64) NULL,
    nickname VARCHAR(40) NOT NULL,
    avatar_file_id BIGINT NULL,
    bio VARCHAR(300) NULL,
    grade_name VARCHAR(32) NULL,
    major_name VARCHAR(80) NULL,
    interest_tags_json JSON NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    token_version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_wechat_openid (wechat_openid),
    KEY idx_user_campus_role_status (campus_id, role, status),
    CONSTRAINT fk_user_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE file_object (
    id BIGINT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id BIGINT NULL,
    bucket_name VARCHAR(80) NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NULL,
    content_type VARCHAR(80) NOT NULL,
    byte_size BIGINT NOT NULL,
    width INT NULL,
    height INT NULL,
    sha256 CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    scan_result VARCHAR(80) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    UNIQUE KEY uk_file_object (bucket_name, object_key),
    KEY idx_file_business (business_type, business_id, status),
    KEY idx_file_owner (owner_id, created_at),
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_file_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE campus_identity_binding (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    campus_id BIGINT NOT NULL,
    identifier_type VARCHAR(32) NOT NULL,
    identifier_fingerprint CHAR(64) NOT NULL,
    identifier_ciphertext TEXT NULL,
    identifier_masked VARCHAR(32) NOT NULL,
    proof_file_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    reviewer_id BIGINT NULL,
    review_reason VARCHAR(500) NULL,
    verified_at DATETIME(3) NULL,
    expires_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    current_user_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN status IN ('PENDING', 'APPROVED') THEN user_id ELSE NULL END
    ) STORED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_identity_fingerprint (campus_id, identifier_type, identifier_fingerprint),
    UNIQUE KEY uk_identity_current_user (current_user_id),
    KEY idx_identity_user_status (user_id, status),
    KEY idx_identity_review (campus_id, status, created_at),
    CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_identity_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_identity_proof FOREIGN KEY (proof_file_id) REFERENCES file_object(id) ON DELETE RESTRICT,
    CONSTRAINT fk_identity_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE buddy_activity (
    id BIGINT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    scene_name VARCHAR(20) NOT NULL,
    title VARCHAR(50) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    meeting_mode VARCHAR(16) NOT NULL,
    public_location VARCHAR(120) NULL,
    member_location_detail VARCHAR(255) NULL,
    join_requirement VARCHAR(500) NULL,
    join_questions_json JSON NULL,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    apply_deadline DATETIME(3) NOT NULL,
    capacity INT NOT NULL,
    accepted_count INT NOT NULL DEFAULT 0,
    review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED',
    lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    reviewer_id BIGINT NULL,
    claim_expires_at DATETIME(3) NULL,
    completion_deadline_at DATETIME(3) NULL,
    review_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    CHECK (capacity BETWEEN 2 AND 50),
    CHECK (accepted_count BETWEEN 0 AND capacity),
    CHECK (end_at > start_at),
    CHECK (apply_deadline <= start_at),
    KEY idx_activity_discover (campus_id, review_status, lifecycle_status, moderation_status, start_at),
    KEY idx_activity_creator (creator_id, created_at),
    KEY idx_activity_review (campus_id, review_status, claim_expires_at, created_at),
    KEY idx_activity_completion (lifecycle_status, completion_deadline_at, created_at),
    CONSTRAINT fk_activity_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_tag (
    id BIGINT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    name VARCHAR(12) NOT NULL,
    normalized_name VARCHAR(24) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_tag_name (campus_id, normalized_name),
    CONSTRAINT fk_tag_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_tag_relation (
    activity_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (activity_id, tag_id),
    CONSTRAINT fk_tag_relation_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_relation_tag FOREIGN KEY (tag_id) REFERENCES activity_tag(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_media (
    activity_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (activity_id, file_id),
    UNIQUE KEY uk_activity_media_order (activity_id, sort_order),
    CONSTRAINT fk_activity_media_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_media_file FOREIGN KEY (file_id) REFERENCES file_object(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE buddy_application (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    answers_json JSON NULL,
    message VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decision_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_application_user (activity_id, applicant_id),
    KEY idx_application_activity_status (activity_id, status, created_at),
    KEY idx_application_user_status (applicant_id, status, created_at),
    CONSTRAINT fk_application_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_application_user FOREIGN KEY (applicant_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE buddy_member (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    completion_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at DATETIME(3) NULL,
    UNIQUE KEY uk_member_user (activity_id, user_id),
    KEY idx_member_user_status (user_id, status, joined_at),
    CONSTRAINT fk_member_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_status_log (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    dimension_name VARCHAR(24) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    action_name VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_status_log_activity (activity_id, created_at),
    CONSTRAINT fk_status_log_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_status_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE conversation (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_conversation_activity (activity_id),
    CONSTRAINT fk_conversation_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    client_message_id VARCHAR(64) NOT NULL,
    message_type VARCHAR(16) NOT NULL,
    content VARCHAR(1000) NULL,
    file_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_message_client (conversation_id, sender_id, client_message_id),
    KEY idx_message_cursor (conversation_id, id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE RESTRICT,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_message_file FOREIGN KEY (file_id) REFERENCES file_object(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(80) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_type VARCHAR(32) NULL,
    target_id BIGINT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_notification_user (user_id, is_read, created_at),
    KEY idx_notification_delivery (delivery_status, next_retry_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE evaluation (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    tags_json JSON NOT NULL,
    rating TINYINT NOT NULL,
    private_note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CHECK (rating BETWEEN 1 AND 5),
    CHECK (reviewer_id <> reviewee_id),
    UNIQUE KEY uk_evaluation_pair (activity_id, reviewer_id, reviewee_id),
    KEY idx_evaluation_reviewee (reviewee_id, created_at),
    CONSTRAINT fk_evaluation_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_reviewee FOREIGN KEY (reviewee_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE favorite_activity (
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, activity_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_activity FOREIGN KEY (activity_id) REFERENCES buddy_activity(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE report_case (
    id BIGINT PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason_code VARCHAR(40) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    assignee_id BIGINT NULL,
    claim_expires_at DATETIME(3) NULL,
    resolution VARCHAR(1000) NULL,
    appeal_reason VARCHAR(1000) NULL,
    appeal_resolution VARCHAR(1000) NULL,
    appealed_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_report_review (campus_id, status, claim_expires_at, created_at),
    KEY idx_report_reporter (reporter_id, created_at),
    CONSTRAINT fk_report_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT,
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_report_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE moderation_action (
    id BIGINT PRIMARY KEY,
    report_id BIGINT NULL,
    operator_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    expires_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_action_target (target_type, target_id, created_at),
    CONSTRAINT fk_action_report FOREIGN KEY (report_id) REFERENCES report_case(id) ON DELETE RESTRICT,
    CONSTRAINT fk_action_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY,
    operator_id BIGINT NULL,
    operator_role VARCHAR(32) NULL,
    campus_id BIGINT NULL,
    action_name VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NULL,
    before_state JSON NULL,
    after_state JSON NULL,
    reason VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_audit_target (target_type, target_id, created_at),
    KEY idx_audit_operator (operator_id, created_at),
    KEY idx_audit_campus (campus_id, created_at),
    CONSTRAINT fk_audit_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_audit_campus FOREIGN KEY (campus_id) REFERENCES campus(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_block (
    blocker_id BIGINT NOT NULL,
    blocked_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id),
    CONSTRAINT fk_block_blocker FOREIGN KEY (blocker_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_block_blocked FOREIGN KEY (blocked_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
