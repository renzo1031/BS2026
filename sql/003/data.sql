USE left_behind_aid;

INSERT INTO sys_department (id, code, name, enabled)
VALUES (1, 'CARE_CENTER', '示范关爱服务中心', 1)
ON DUPLICATE KEY UPDATE code = VALUES(code), name = VALUES(name), enabled = VALUES(enabled);

INSERT INTO sys_role (id, code, name, data_scope) VALUES
    (1, 'SYS_ADMIN', '系统管理员', 'GLOBAL'),
    (2, 'SUPERVISOR', '部门主管', 'DEPARTMENT'),
    (3, 'CASE_WORKER', '个案人员', 'DEPARTMENT'),
    (4, 'VOLUNTEER', '志愿者', 'SELF')
ON DUPLICATE KEY UPDATE name = VALUES(name), data_scope = VALUES(data_scope);

INSERT INTO sys_permission (id, code, name, module) VALUES
    (1, 'system:user:manage', '用户管理', 'system'),
    (2, 'system:role:manage', '角色权限管理', 'system'),
    (3, 'system:audit:read', '审计日志查看', 'system'),
    (4, 'dashboard:read', '工作台查看', 'dashboard'),
    (5, 'child:read', '儿童档案查看', 'child'),
    (6, 'child:write', '儿童档案维护', 'child'),
    (7, 'child:review', '儿童档案审核', 'child'),
    (8, 'child:archive', '儿童档案归档', 'child'),
    (9, 'aid:read', '帮扶需求查看', 'aid'),
    (10, 'aid:write', '帮扶需求维护', 'aid'),
    (11, 'aid:review', '帮扶需求审核', 'aid'),
    (12, 'aid:match', '志愿者匹配', 'aid'),
    (13, 'volunteer:profile', '志愿者资料维护', 'volunteer'),
    (14, 'volunteer:review', '志愿者认证审核', 'volunteer'),
    (15, 'application:apply', '申请帮扶需求', 'application'),
    (16, 'application:read', '申请记录查看', 'application'),
    (17, 'application:manage', '申请匹配管理', 'application'),
    (18, 'assignment:read', '服务任务查看', 'assignment'),
    (19, 'assignment:execute', '服务任务执行', 'assignment'),
    (20, 'assignment:confirm', '服务任务验收', 'assignment'),
    (21, 'feedback:write', '服务评价', 'feedback')
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 9), (1, 14),
    (2, 4), (2, 5), (2, 7), (2, 9), (2, 11), (2, 12), (2, 16), (2, 17), (2, 18), (2, 20),
    (3, 4), (3, 5), (3, 6), (3, 8), (3, 9), (3, 10), (3, 18), (3, 21),
    (4, 4), (4, 9), (4, 13), (4, 15), (4, 16), (4, 18), (4, 19);

-- Default accounts use password 123456. Change it after the first deployment.
INSERT INTO sys_user (id, department_id, role_id, username, password_hash, display_name, status) VALUES
    (1, NULL, 1, 'admin', '$2b$12$Lo/HtBcyESgPVbYzXsYsEu29.Q8iUINGdaUMreqAtVA1/emMROKoG', '系统管理员', 'ACTIVE'),
    (2, 1, 2, 'supervisor', '$2b$12$Lo/HtBcyESgPVbYzXsYsEu29.Q8iUINGdaUMreqAtVA1/emMROKoG', '示范中心主管', 'ACTIVE'),
    (3, 1, 3, 'worker', '$2b$12$Lo/HtBcyESgPVbYzXsYsEu29.Q8iUINGdaUMreqAtVA1/emMROKoG', '个案工作人员', 'ACTIVE'),
    (4, NULL, 4, 'volunteer', '$2b$12$Lo/HtBcyESgPVbYzXsYsEu29.Q8iUINGdaUMreqAtVA1/emMROKoG', '测试志愿者', 'ACTIVE')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), status = VALUES(status);

INSERT INTO volunteer_profile (user_id, certification_status)
VALUES (4, 'UNVERIFIED')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);
