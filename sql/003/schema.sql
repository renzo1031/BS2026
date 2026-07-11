CREATE DATABASE IF NOT EXISTS left_behind_aid
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE left_behind_aid;

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_department_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(50) NOT NULL,
    data_scope VARCHAR(20) NOT NULL,
    CONSTRAINT uk_role_code UNIQUE (code),
    CONSTRAINT ck_role_scope CHECK (data_scope IN ('GLOBAL', 'DEPARTMENT', 'SELF'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    module VARCHAR(32) NOT NULL,
    CONSTRAINT uk_permission_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    department_id BIGINT NULL,
    role_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME(3) NULL,
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
    CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    INDEX idx_user_department_status (department_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_session (
    id CHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_seen_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    INDEX idx_session_user_active (user_id, revoked_at, expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS child_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_no VARCHAR(32) NOT NULL,
    department_id BIGINT NOT NULL,
    name_encrypted TEXT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    birth_date DATE NOT NULL,
    region VARCHAR(100) NOT NULL,
    school_stage VARCHAR(30) NOT NULL,
    guardian_name_encrypted TEXT NOT NULL,
    guardian_phone_encrypted TEXT NOT NULL,
    address_encrypted TEXT NOT NULL,
    family_summary VARCHAR(1000) NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(500) NULL,
    created_by BIGINT NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_child_file_no UNIQUE (file_no),
    CONSTRAINT fk_child_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
    CONSTRAINT fk_child_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
    CONSTRAINT fk_child_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id),
    CONSTRAINT ck_child_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_child_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_child_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'REJECTED', 'ARCHIVED')),
    INDEX idx_child_department_status (department_id, status, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS volunteer_profile (
    user_id BIGINT PRIMARY KEY,
    real_name_encrypted TEXT NULL,
    phone_encrypted TEXT NULL,
    service_region VARCHAR(100) NULL,
    skills VARCHAR(500) NULL,
    available_time VARCHAR(300) NULL,
    introduction VARCHAR(1000) NULL,
    certification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    rejection_reason VARCHAR(500) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_volunteer_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_volunteer_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id),
    CONSTRAINT ck_volunteer_status CHECK (certification_status IN ('UNVERIFIED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    INDEX idx_volunteer_status (certification_status, updated_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aid_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(32) NOT NULL,
    child_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    public_summary VARCHAR(300) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(500) NULL,
    created_by BIGINT NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_aid_request_no UNIQUE (request_no),
    CONSTRAINT fk_aid_child FOREIGN KEY (child_id) REFERENCES child_profile(id),
    CONSTRAINT fk_aid_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
    CONSTRAINT fk_aid_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
    CONSTRAINT fk_aid_reviewer FOREIGN KEY (reviewed_by) REFERENCES sys_user(id),
    CONSTRAINT ck_aid_category CHECK (category IN ('EDUCATION', 'COMPANIONSHIP', 'LIFE_CARE', 'SAFETY', 'PSYCHOLOGICAL', 'OTHER')),
    CONSTRAINT ck_aid_priority CHECK (priority IN ('NORMAL', 'URGENT')),
    CONSTRAINT ck_aid_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'MATCHED', 'IN_PROGRESS', 'PENDING_ACCEPTANCE', 'COMPLETED', 'CLOSED', 'CANCELLED')),
    INDEX idx_aid_department_status (department_id, status, created_at),
    INDEX idx_aid_public_hall (status, category, priority, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aid_request_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL,
    comment VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_review_request FOREIGN KEY (request_id) REFERENCES aid_request(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id),
    CONSTRAINT ck_review_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    INDEX idx_review_request_time (request_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS aid_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    volunteer_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    decided_by BIGINT NULL,
    decided_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_application_request_volunteer UNIQUE (request_id, volunteer_id),
    CONSTRAINT fk_application_request FOREIGN KEY (request_id) REFERENCES aid_request(id),
    CONSTRAINT fk_application_volunteer FOREIGN KEY (volunteer_id) REFERENCES sys_user(id),
    CONSTRAINT fk_application_decider FOREIGN KEY (decided_by) REFERENCES sys_user(id),
    CONSTRAINT ck_application_status CHECK (status IN ('APPLIED', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    INDEX idx_application_volunteer_status (volunteer_id, status, created_at),
    INDEX idx_application_request_status (request_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    volunteer_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    active_marker TINYINT NULL DEFAULT 1,
    started_at DATETIME(3) NULL,
    completion_summary VARCHAR(1500) NULL,
    submitted_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    terminated_reason VARCHAR(500) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_assignment_active UNIQUE (request_id, active_marker),
    CONSTRAINT uk_assignment_application UNIQUE (application_id),
    CONSTRAINT fk_assignment_request FOREIGN KEY (request_id) REFERENCES aid_request(id),
    CONSTRAINT fk_assignment_application FOREIGN KEY (application_id) REFERENCES aid_application(id),
    CONSTRAINT fk_assignment_volunteer FOREIGN KEY (volunteer_id) REFERENCES sys_user(id),
    CONSTRAINT fk_assignment_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
    CONSTRAINT ck_assignment_status CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'PENDING_ACCEPTANCE', 'COMPLETED', 'TERMINATED')),
    INDEX idx_assignment_volunteer_status (volunteer_id, status, created_at),
    INDEX idx_assignment_department_status (department_id, status, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    service_date DATE NOT NULL,
    duration_minutes INT NOT NULL,
    content VARCHAR(1500) NOT NULL,
    result VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_visit_assignment FOREIGN KEY (assignment_id) REFERENCES service_assignment(id),
    CONSTRAINT fk_visit_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
    CONSTRAINT ck_visit_duration CHECK (duration_minutes BETWEEN 1 AND 1440),
    INDEX idx_visit_assignment_date (assignment_id, service_date, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS service_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_feedback_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_feedback_assignment FOREIGN KEY (assignment_id) REFERENCES service_assignment(id),
    CONSTRAINT fk_feedback_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
    CONSTRAINT ck_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    business_type VARCHAR(40) NOT NULL,
    business_id VARCHAR(64) NULL,
    before_status VARCHAR(30) NULL,
    after_status VARCHAR(30) NULL,
    detail VARCHAR(500) NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    INDEX idx_audit_business (business_type, business_id, created_at),
    INDEX idx_audit_user_time (user_id, created_at)
) ENGINE=InnoDB;
