CREATE DATABASE IF NOT EXISTS campus_lost_found
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_lost_found;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS lf_custody_handover;
DROP TABLE IF EXISTS lf_clue_feedback;
DROP TABLE IF EXISTS lf_claim_application;
DROP TABLE IF EXISTS lf_item_image;
DROP TABLE IF EXISTS lf_item;
DROP TABLE IF EXISTS lf_location;
DROP TABLE IF EXISTS lf_category;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(120) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  student_no VARCHAR(30) NULL,
  email VARCHAR(100) NULL,
  avatar_url VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  last_login_time DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_phone (phone),
  UNIQUE KEY uk_sys_user_student_no (student_no),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY,
  role_code VARCHAR(30) NOT NULL,
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(255) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_user (user_id),
  KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(60) NOT NULL,
  menu_code VARCHAR(80) NOT NULL,
  path VARCHAR(120) NULL,
  component VARCHAR(120) NULL,
  permission_code VARCHAR(100) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  visible TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sys_menu_code (menu_code),
  KEY idx_sys_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

CREATE TABLE lf_category (
  id BIGINT PRIMARY KEY,
  category_name VARCHAR(60) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lf_category_name (category_name),
  KEY idx_lf_category_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品分类表';

CREATE TABLE lf_location (
  id BIGINT PRIMARY KEY,
  location_name VARCHAR(80) NOT NULL,
  area_name VARCHAR(80) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lf_location_name (location_name),
  KEY idx_lf_location_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校园地点表';

CREATE TABLE lf_item (
  id BIGINT PRIMARY KEY,
  item_no VARCHAR(40) NOT NULL,
  type VARCHAR(20) NOT NULL,
  title VARCHAR(120) NOT NULL,
  category_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  event_time DATETIME NOT NULL,
  description TEXT NOT NULL,
  contact_name VARCHAR(50) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  publisher_id BIGINT NOT NULL,
  reviewer_id BIGINT NULL,
  review_time DATETIME NULL,
  review_result VARCHAR(30) NULL,
  review_reason VARCHAR(500) NULL,
  current_claimant_id BIGINT NULL,
  custodian_id BIGINT NULL,
  custody_location VARCHAR(120) NULL,
  completed_time DATETIME NULL,
  offline_reason VARCHAR(500) NULL,
  last_operator_id BIGINT NULL,
  last_operation_summary VARCHAR(255) NULL,
  last_operation_time DATETIME NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lf_item_no (item_no),
  KEY idx_lf_item_type (type),
  KEY idx_lf_item_status (status),
  KEY idx_lf_item_category (category_id),
  KEY idx_lf_item_location (location_id),
  KEY idx_lf_item_publisher (publisher_id),
  KEY idx_lf_item_reviewer (reviewer_id),
  KEY idx_lf_item_claimant (current_claimant_id),
  KEY idx_lf_item_custodian (custodian_id),
  KEY idx_lf_item_last_operation_time (last_operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='失物招领物品主表';

CREATE TABLE lf_item_image (
  id BIGINT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_lf_item_image_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物品图片表';

CREATE TABLE lf_claim_application (
  id BIGINT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  applicant_name VARCHAR(50) NOT NULL,
  applicant_phone VARCHAR(20) NOT NULL,
  proof_text TEXT NOT NULL,
  proof_image_url VARCHAR(255) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  reviewer_id BIGINT NULL,
  review_time DATETIME NULL,
  review_reason VARCHAR(500) NULL,
  canceled_reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_lf_claim_item (item_id),
  KEY idx_lf_claim_applicant (applicant_id),
  KEY idx_lf_claim_status (status),
  KEY idx_lf_claim_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认领申请表';

CREATE TABLE lf_clue_feedback (
  id BIGINT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  submitter_id BIGINT NOT NULL,
  clue_content TEXT NOT NULL,
  clue_image_url VARCHAR(255) NULL,
  contact_phone VARCHAR(20) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRM',
  confirmer_id BIGINT NULL,
  confirm_time DATETIME NULL,
  confirm_reason VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_lf_clue_item (item_id),
  KEY idx_lf_clue_submitter (submitter_id),
  KEY idx_lf_clue_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线索反馈表';

CREATE TABLE lf_custody_handover (
  id BIGINT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  claim_id BIGINT NULL,
  custodian_id BIGINT NOT NULL,
  custody_location VARCHAR(120) NOT NULL,
  receiver_id BIGINT NULL,
  receiver_name VARCHAR(50) NULL,
  receiver_phone VARCHAR(20) NULL,
  handover_location VARCHAR(120) NULL,
  handover_time DATETIME NULL,
  handler_id BIGINT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'CUSTODY',
  remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_lf_handover_item (item_id),
  KEY idx_lf_handover_claim (claim_id),
  KEY idx_lf_handover_custodian (custodian_id),
  KEY idx_lf_handover_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保管交接表';

CREATE TABLE sys_notice (
  id BIGINT PRIMARY KEY,
  notice_type VARCHAR(30) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  receiver_id BIGINT NULL,
  read_status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
  publish_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  published_at DATETIME NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  popup_enabled TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_sys_notice_type (notice_type),
  KEY idx_sys_notice_receiver (receiver_id),
  KEY idx_sys_notice_read_status (read_status),
  KEY idx_sys_notice_publish_window (publish_status, start_time, end_time),
  KEY idx_sys_notice_popup (popup_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告与站内通知表';

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY,
  operator_id BIGINT NULL,
  operator_name VARCHAR(50) NULL,
  operator_role VARCHAR(30) NULL,
  target_type VARCHAR(40) NOT NULL,
  target_id BIGINT NULL,
  action VARCHAR(60) NOT NULL,
  before_status VARCHAR(40) NULL,
  after_status VARCHAR(40) NULL,
  result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  reason VARCHAR(500) NULL,
  request_ip VARCHAR(60) NULL,
  user_agent VARCHAR(255) NULL,
  request_path VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sys_log_operator (operator_id),
  KEY idx_sys_log_target (target_type, target_id),
  KEY idx_sys_log_action (action),
  KEY idx_sys_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;

-- BCrypt hash for default password: password
SET @DEFAULT_PASSWORD_HASH = '$2a$10$tb8TILe98WADMpvBitufyupaypnAYaTCzWFEbWmItbkPhmXldVgHO';

INSERT INTO sys_user (id, username, password_hash, real_name, phone, student_no, email, status, created_by, updated_by)
VALUES
  (1, 'admin', @DEFAULT_PASSWORD_HASH, '系统管理员', '13800000001', 'A0001', 'admin@example.com', 'ENABLED', 1, 1),
  (2, 'staff', @DEFAULT_PASSWORD_HASH, '物品保管员', '13800000002', 'S0001', 'staff@example.com', 'ENABLED', 1, 1),
  (3, 'user', @DEFAULT_PASSWORD_HASH, '普通用户', '13800000003', 'U0001', 'user@example.com', 'ENABLED', 1, 1),
  (4, 'student01', @DEFAULT_PASSWORD_HASH, '李同学', '13800000004', '20260001', 'student01@example.com', 'ENABLED', 1, 1);

INSERT INTO sys_role (id, role_code, role_name, description, created_by, updated_by)
VALUES
  (1, 'ADMIN', '管理员', '系统管理、审核和审计', 1, 1),
  (2, 'STAFF', '物品保管员', '认领核验、保管和交接', 1, 1),
  (3, 'USER', '普通用户', '发布、认领和线索反馈', 1, 1);

INSERT INTO sys_user_role (id, user_id, role_id, created_by, updated_by)
VALUES
  (1, 1, 1, 1, 1),
  (2, 2, 2, 1, 1),
  (3, 3, 3, 1, 1),
  (4, 4, 3, 1, 1);

INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, path, component, permission_code, sort_order, created_by, updated_by)
VALUES
  (1, 0, '后台首页', 'admin_dashboard', '/admin', 'admin/Dashboard', 'admin:dashboard:view', 1, 1, 1),
  (2, 0, '物品审核', 'admin_item_review', '/admin/item-review', 'admin/ItemReview', 'admin:item:review', 2, 1, 1),
  (3, 0, '全量物品', 'admin_items', '/admin/items', 'admin/ItemManage', 'admin:item:view', 3, 1, 1),
  (4, 0, '认领管理', 'admin_claims', '/admin/claims', 'admin/ClaimManage', 'admin:claim:view', 4, 1, 1),
  (5, 0, '线索管理', 'admin_clues', '/admin/clues', 'admin:ClueManage', 'admin:clue:view', 5, 1, 1),
  (6, 0, '保管交接', 'staff_handover', '/staff/handover', 'staff/HandoverManage', 'staff:handover:view', 6, 1, 1),
  (7, 0, '用户管理', 'admin_users', '/admin/users', 'admin/UserManage', 'admin:user:view', 7, 1, 1),
  (8, 0, '分类地点', 'admin_taxonomy', '/admin/taxonomy', 'admin/TaxonomyManage', 'admin:taxonomy:view', 8, 1, 1),
  (9, 0, '公告管理', 'admin_notices', '/admin/notices', 'admin/NoticeManage', 'admin:notice:view', 9, 1, 1),
  (10, 0, '操作日志', 'admin_logs', '/admin/logs', 'admin/OperationLog', 'admin:log:view', 10, 1, 1);

INSERT INTO lf_category (id, category_name, sort_order, status, created_by, updated_by)
VALUES
  (1, '证件卡片', 1, 'ENABLED', 1, 1),
  (2, '电子产品', 2, 'ENABLED', 1, 1),
  (3, '书籍文具', 3, 'ENABLED', 1, 1),
  (4, '生活用品', 4, 'ENABLED', 1, 1),
  (5, '衣物饰品', 5, 'ENABLED', 1, 1),
  (6, '其他', 99, 'ENABLED', 1, 1);

INSERT INTO lf_location (id, location_name, area_name, sort_order, status, created_by, updated_by)
VALUES
  (1, '图书馆', '学习区', 1, 'ENABLED', 1, 1),
  (2, '第一教学楼', '教学区', 2, 'ENABLED', 1, 1),
  (3, '学生食堂', '生活区', 3, 'ENABLED', 1, 1),
  (4, '学生宿舍区', '生活区', 4, 'ENABLED', 1, 1),
  (5, '操场', '运动区', 5, 'ENABLED', 1, 1),
  (6, '校门口', '出入口', 6, 'ENABLED', 1, 1),
  (7, '实验楼', '实验区', 7, 'ENABLED', 1, 1);

INSERT INTO lf_item (
  id, item_no, type, title, category_id, location_id, event_time, description,
  contact_name, contact_phone, status, publisher_id, reviewer_id, review_time,
  review_result, review_reason, current_claimant_id, custodian_id, custody_location,
  last_operator_id, last_operation_summary, last_operation_time, created_by, updated_by
)
VALUES
  (1, 'FOUND202607040001', 'FOUND', '黑色无线耳机', 2, 1, '2026-07-04 09:20:00',
   '在图书馆二楼自习区拾到黑色无线耳机，充电盒外侧有轻微划痕。',
   '李同学', '13800000004', 'HANDOVER_PENDING', 4, 1, '2026-07-04 10:00:00',
   'APPROVED', '信息完整，通过上架。', 3, 2, '校门口服务中心 A 柜',
   2, '保管员核验认领通过，待线下交接', '2026-07-04 11:30:00', 4, 2),
  (2, 'LOST202607040001', 'LOST', '蓝色校园卡', 1, 3, '2026-07-04 12:10:00',
   '午饭后发现校园卡遗失，卡套是透明软壳。',
   '普通用户', '13800000003', 'PUBLISHED', 3, 1, '2026-07-04 13:00:00',
   'APPROVED', '寻物信息完整。', NULL, NULL, NULL,
   1, '管理员审核通过并上架', '2026-07-04 13:00:00', 3, 1),
  (3, 'FOUND202607040002', 'FOUND', '高数教材', 3, 2, '2026-07-04 14:30:00',
   '第一教学楼 301 教室讲台下拾到高等数学教材，内有课堂笔记。',
   '普通用户', '13800000003', 'PENDING_REVIEW', 3, NULL, NULL,
   NULL, NULL, NULL, NULL, NULL,
   3, '用户提交审核', '2026-07-04 14:40:00', 3, 3);

INSERT INTO lf_item_image (id, item_id, image_url, sort_order, created_by, updated_by)
VALUES
  (1, 1, '/uploads/items/demo-earphone.jpg', 1, 4, 4),
  (2, 2, '/uploads/items/demo-card.jpg', 1, 3, 3),
  (3, 3, '/uploads/items/demo-book.jpg', 1, 3, 3);

INSERT INTO lf_claim_application (
  id, item_id, applicant_id, applicant_name, applicant_phone, proof_text, proof_image_url,
  status, reviewer_id, review_time, review_reason, created_by, updated_by
)
VALUES
  (1, 1, 3, '普通用户', '13800000003', '耳机蓝牙名称和购买时间可以匹配，能说出耳机盒划痕位置。',
   NULL, 'APPROVED', 2, '2026-07-04 11:30:00', '证明信息匹配，等待线下交接。', 3, 2);

INSERT INTO lf_clue_feedback (
  id, item_id, submitter_id, clue_content, clue_image_url, contact_phone,
  status, confirmer_id, confirm_time, confirm_reason, created_by, updated_by
)
VALUES
  (1, 2, 4, '我在食堂二楼靠窗座位附近看到过一张蓝色校园卡，已交给食堂服务台。',
   NULL, '13800000004', 'PENDING_CONFIRM', NULL, NULL, NULL, 4, 4);

INSERT INTO lf_custody_handover (
  id, item_id, claim_id, custodian_id, custody_location, receiver_id, receiver_name,
  receiver_phone, handover_location, handover_time, handler_id, status, remark, created_by, updated_by
)
VALUES
  (1, 1, 1, 2, '校门口服务中心 A 柜', 3, '普通用户',
   '13800000003', NULL, NULL, 2, 'HANDOVER_PENDING', '待申请人到服务中心领取。', 2, 2);

INSERT INTO sys_notice (
  id, notice_type, title, content, receiver_id, read_status, publish_status, published_at, start_time, end_time, popup_enabled, created_by, updated_by
)
VALUES
  (1, 'ANNOUNCEMENT', '校园失物招领中心试运行', '请同学们发布信息时填写准确地点、时间和联系方式，认领时准备能够证明归属的材料。', NULL, 'UNREAD', 'PUBLISHED', '2026-07-04 08:00:00', '2026-07-04 08:00:00', NULL, 1, 1, 1),
  (2, 'CLAIM', '认领申请已通过核验', '你的黑色无线耳机认领申请已通过，请到校门口服务中心 A 柜办理交接。', 3, 'UNREAD', 'PUBLISHED', '2026-07-04 11:30:00', '2026-07-04 11:30:00', NULL, 0, 2, 2);

INSERT INTO sys_operation_log (
  id, operator_id, operator_name, operator_role, target_type, target_id, action,
  before_status, after_status, result, reason, request_ip, user_agent, request_path, created_at
)
VALUES
  (1, 4, '李同学', 'USER', 'ITEM', 1, 'CREATE', NULL, 'DRAFT', 'SUCCESS', '创建招领信息', '127.0.0.1', 'seed', '/api/items', '2026-07-04 09:30:00'),
  (2, 4, '李同学', 'USER', 'ITEM', 1, 'SUBMIT_REVIEW', 'DRAFT', 'PENDING_REVIEW', 'SUCCESS', '提交审核', '127.0.0.1', 'seed', '/api/items/1/submit', '2026-07-04 09:35:00'),
  (3, 1, '系统管理员', 'ADMIN', 'ITEM', 1, 'REVIEW_APPROVE', 'PENDING_REVIEW', 'PUBLISHED', 'SUCCESS', '信息完整，通过上架。', '127.0.0.1', 'seed', '/api/admin/items/1/review', '2026-07-04 10:00:00'),
  (4, 3, '普通用户', 'USER', 'CLAIM', 1, 'CLAIM_CREATE', NULL, 'PENDING', 'SUCCESS', '提交认领申请', '127.0.0.1', 'seed', '/api/items/1/claims', '2026-07-04 11:00:00'),
  (5, 2, '物品保管员', 'STAFF', 'CLAIM', 1, 'CLAIM_APPROVE', 'PENDING', 'APPROVED', 'SUCCESS', '证明信息匹配，等待线下交接。', '127.0.0.1', 'seed', '/api/staff/claims/1/approve', '2026-07-04 11:30:00'),
  (6, 2, '物品保管员', 'STAFF', 'ITEM', 1, 'CLAIM_APPROVE', 'CLAIM_REVIEWING', 'HANDOVER_PENDING', 'SUCCESS', '保管员核验认领通过，待线下交接', '127.0.0.1', 'seed', '/api/staff/claims/1/approve', '2026-07-04 11:30:00'),
  (7, 1, '系统管理员', 'ADMIN', 'ITEM', 2, 'REVIEW_APPROVE', 'PENDING_REVIEW', 'PUBLISHED', 'SUCCESS', '寻物信息完整。', '127.0.0.1', 'seed', '/api/admin/items/2/review', '2026-07-04 13:00:00'),
  (8, 4, '李同学', 'USER', 'CLUE', 1, 'CLUE_CREATE', NULL, 'PENDING_CONFIRM', 'SUCCESS', '提交寻物线索', '127.0.0.1', 'seed', '/api/items/2/clues', '2026-07-04 13:30:00');
