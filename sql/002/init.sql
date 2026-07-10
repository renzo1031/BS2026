CREATE DATABASE IF NOT EXISTS campus_service_platform
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE campus_service_platform;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS feedback;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS attachment;
DROP TABLE IF EXISTS process_record;
DROP TABLE IF EXISTS service_request;
DROP TABLE IF EXISTS service_item;
DROP TABLE IF EXISTS service_category;
DROP TABLE IF EXISTS venue;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS student_identity;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_department;

CREATE TABLE sys_department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  contact_name VARCHAR(40),
  phone VARCHAR(30),
  description VARCHAR(255),
  UNIQUE KEY uk_department_name (name)
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  student_no VARCHAR(50),
  phone VARCHAR(30),
  email VARCHAR(100),
  college VARCHAR(100),
  major VARCHAR(100),
  department_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  token_version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_student_no (student_no),
  INDEX idx_user_department (department_id),
  CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
  CONSTRAINT chk_user_status CHECK (status IN ('ENABLED', 'DISABLED')),
  CONSTRAINT chk_user_token_version CHECK (token_version >= 0),
  CONSTRAINT chk_user_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE student_identity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_no VARCHAR(50) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  college VARCHAR(100) NOT NULL,
  major VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  bound_user_id BIGINT,
  UNIQUE KEY uk_student_identity_no (student_no),
  UNIQUE KEY uk_student_identity_bound_user (bound_user_id),
  CONSTRAINT fk_student_identity_user FOREIGN KEY (bound_user_id) REFERENCES sys_user(id),
  CONSTRAINT chk_student_identity_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(60) NOT NULL,
  description VARCHAR(255),
  UNIQUE KEY uk_role_code (code)
);

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE service_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  description VARCHAR(255),
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_category_code (code),
  CONSTRAINT chk_category_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE venue (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  location VARCHAR(120) NOT NULL,
  capacity INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  description VARCHAR(255),
  INDEX idx_venue_status (status),
  CONSTRAINT chk_venue_capacity CHECK (capacity > 0),
  CONSTRAINT chk_venue_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE'))
);

CREATE TABLE service_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(40) NOT NULL,
  description VARCHAR(255),
  required_materials VARCHAR(255),
  need_venue TINYINT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_service_item_code (code),
  INDEX idx_item_category (category_id),
  INDEX idx_item_department (department_id),
  CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES service_category(id),
  CONSTRAINT fk_item_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
  CONSTRAINT chk_item_type CHECK (type IN ('REPAIR', 'CERTIFICATE', 'VENUE')),
  CONSTRAINT chk_item_need_venue CHECK (need_venue IN (0, 1)),
  CONSTRAINT chk_item_enabled CHECK (enabled IN (0, 1))
);

CREATE TABLE service_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_no VARCHAR(60) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  item_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  handler_id BIGINT,
  venue_id BIGINT,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  location VARCHAR(120),
  repair_category VARCHAR(50),
  urgency VARCHAR(20),
  certificate_type VARCHAR(50),
  purpose VARCHAR(255),
  language VARCHAR(20),
  copies INT,
  delivery_method VARCHAR(30),
  certificate_no VARCHAR(60),
  verification_code VARCHAR(60),
  event_name VARCHAR(120),
  appointment_start DATETIME,
  appointment_end DATETIME,
  attendee_count INT,
  contact_name VARCHAR(50),
  contact_phone VARCHAR(30),
  status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
  result VARCHAR(500),
  accepted_at DATETIME,
  finished_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_request_no (request_no),
  UNIQUE KEY uk_request_certificate_no (certificate_no),
  UNIQUE KEY uk_request_verification_code (verification_code),
  INDEX idx_request_applicant_status_created (applicant_id, status, created_at),
  INDEX idx_request_department_status_created (department_id, status, created_at),
  INDEX idx_request_venue_period (venue_id, appointment_start, appointment_end, status),
  INDEX idx_request_handler (handler_id),
  CONSTRAINT fk_request_item FOREIGN KEY (item_id) REFERENCES service_item(id),
  CONSTRAINT fk_request_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_request_department FOREIGN KEY (department_id) REFERENCES sys_department(id),
  CONSTRAINT fk_request_handler FOREIGN KEY (handler_id) REFERENCES sys_user(id),
  CONSTRAINT fk_request_venue FOREIGN KEY (venue_id) REFERENCES venue(id),
  CONSTRAINT chk_request_version CHECK (version >= 0),
  CONSTRAINT chk_request_status CHECK (status IN (
    'SUBMITTED', 'ACCEPTED', 'PROCESSING', 'FINISHED', 'EVALUATED', 'REJECTED', 'CANCELLED'
  )),
  CONSTRAINT chk_request_copies CHECK (copies IS NULL OR copies > 0),
  CONSTRAINT chk_request_attendee_count CHECK (attendee_count IS NULL OR attendee_count > 0),
  CONSTRAINT chk_request_appointment CHECK (
    (appointment_start IS NULL AND appointment_end IS NULL)
    OR (appointment_start IS NOT NULL AND appointment_end IS NOT NULL AND appointment_end > appointment_start)
  ),
  CONSTRAINT chk_request_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE process_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30) NOT NULL,
  action VARCHAR(80) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_record_request_time (request_id, created_at),
  INDEX idx_record_operator (operator_id),
  CONSTRAINT fk_record_request FOREIGN KEY (request_id) REFERENCES service_request(id),
  CONSTRAINT fk_record_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id),
  CONSTRAINT chk_record_from_status CHECK (
    from_status IS NULL OR from_status IN (
      'SUBMITTED', 'ACCEPTED', 'PROCESSING', 'FINISHED', 'EVALUATED', 'REJECTED', 'CANCELLED'
    )
  ),
  CONSTRAINT chk_record_to_status CHECK (to_status IN (
    'SUBMITTED', 'ACCEPTED', 'PROCESSING', 'FINISHED', 'EVALUATED', 'REJECTED', 'CANCELLED'
  ))
);

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  request_id BIGINT,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notice_user_read_time (user_id, read_flag, created_at),
  INDEX idx_notice_request (request_id),
  CONSTRAINT fk_notice_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_notice_request FOREIGN KEY (request_id) REFERENCES service_request(id),
  CONSTRAINT chk_notice_read_flag CHECK (read_flag IN (0, 1))
);

CREATE TABLE feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  score INT NOT NULL,
  content VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feedback_request (request_id),
  INDEX idx_feedback_user (user_id),
  CONSTRAINT fk_feedback_request FOREIGN KEY (request_id) REFERENCES service_request(id),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT chk_feedback_score CHECK (score BETWEEN 1 AND 5)
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  module VARCHAR(60) NOT NULL,
  action VARCHAR(80) NOT NULL,
  detail VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_log_user_time (user_id, created_at),
  INDEX idx_log_module_time (module, created_at),
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

INSERT INTO sys_department (id, name, contact_name, phone, description) VALUES
(1, '学生事务中心', '张老师', '020-10000001', '负责在读证明等学生事务'),
(2, '后勤服务中心', '李老师', '020-10000002', '负责宿舍维修和后勤保障'),
(3, '团委活动管理部', '周老师', '020-10000003', '负责学生活动与场地审批');

INSERT INTO sys_role (id, code, name, description) VALUES
(1, 'STUDENT', '学生', '提交和跟踪本人服务申请'),
(2, 'STAFF', '部门处理人员', '处理本部门服务事项'),
(3, 'ADMIN', '系统管理员', '维护平台基础数据和查看全局记录');

-- 默认密码均为 123456。
INSERT INTO sys_user (
  id, username, password_hash, real_name, student_no, phone, email, college, major,
  department_id, status, token_version
) VALUES
(1, 'student', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '陈同学', '20260001', '13800000001', 'student@example.com', '计算机学院', '软件工程', NULL, 'ENABLED', 0),
(2, 'student2', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '李同学', '20260002', '13800000002', 'student2@example.com', '信息学院', '信息管理', NULL, 'ENABLED', 0),
(3, 'repair_staff', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '后勤老师', NULL, '13800000003', 'repair.staff@example.com', NULL, NULL, 2, 'ENABLED', 0),
(4, 'cert_staff', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '事务老师', NULL, '13800000004', 'cert.staff@example.com', NULL, NULL, 1, 'ENABLED', 0),
(5, 'venue_staff', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '团委老师', NULL, '13800000005', 'venue.staff@example.com', NULL, NULL, 3, 'ENABLED', 0),
(6, 'admin', '$2b$12$KV8Kv/TLIUFNf1Os0wxq.ONsPld5dcD94h17RGRe/pDf4fQnX64Za', '管理员', NULL, '13800000006', 'admin@example.com', NULL, NULL, NULL, 'ENABLED', 0);

INSERT INTO student_identity (id, student_no, real_name, college, major, status, bound_user_id) VALUES
(1, '20260001', '陈同学', '计算机学院', '软件工程', 'ACTIVE', 1),
(2, '20260002', '李同学', '信息学院', '信息管理', 'ACTIVE', 2),
(3, '20260003', '王同学', '外国语学院', '英语', 'ACTIVE', NULL);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 1),
(3, 2),
(4, 2),
(5, 2),
(6, 3);

INSERT INTO service_category (id, code, name, description, sort_no, enabled) VALUES
(1, 'LIFE', '生活服务', '宿舍、维修、后勤相关服务', 1, 1),
(2, 'STUDY', '学籍证明', '在读证明等材料办理', 2, 1),
(3, 'ACTIVITY', '活动服务', '学生活动场地申请', 3, 1);

INSERT INTO venue (id, name, location, capacity, status, description) VALUES
(1, '大学生活动中心 A101', '学生活动中心一楼', 80, 'AVAILABLE', '适合班会、社团例会'),
(2, '报告厅 B201', '综合楼二楼', 200, 'AVAILABLE', '适合讲座、宣讲会'),
(3, '户外广场', '图书馆前广场', 500, 'AVAILABLE', '适合大型户外活动');

INSERT INTO service_item (
  id, category_id, department_id, code, name, type, description, required_materials, need_venue, enabled
) VALUES
(1, 1, 2, 'DORM_REPAIR', '宿舍报修', 'REPAIR', '由后勤服务中心受理并办结宿舍维修申请', '故障位置、故障类型、紧急程度和问题描述', 0, 1),
(2, 2, 1, 'STUDENT_CERT', '在读证明办理', 'CERTIFICATE', '由学生事务中心审核并生成电子证明', '证明类型、用途、语言、份数和领取方式', 0, 1),
(3, 3, 3, 'VENUE_APPLY', '活动场地申请', 'VENUE', '由团委活动管理部审核活动场地申请', '活动名称、起止时间、人数和联系人信息', 1, 1);
