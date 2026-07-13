SET NAMES utf8mb4;

INSERT INTO campus (id, name, code, status, identity_label) VALUES
    (1, '示范大学', 'DEMO', 'ACTIVE', '学号');

-- Local test accounts only. Production passwords must be injected/reset.
INSERT INTO sys_user
    (id, campus_id, username, password_hash, wechat_openid, nickname, role, status, verification_status, token_version)
VALUES
    (1, NULL, 'admin', '$2b$12$sepHTem6BKVAMLEs66okp.vOZRGjf/Qam8QmEu2MYWCy8XaoWmxSG', NULL, '平台管理员', 'PLATFORM_ADMIN', 'ACTIVE', 'APPROVED', 0),
    (2, 1, 'reviewer', '$2b$12$ZQHoTsOzhaPspkqZLtXMierETlgGeFPqK6pN0WVblI/S4qgXMGnOS', NULL, '校园审核员', 'CAMPUS_REVIEWER', 'ACTIVE', 'APPROVED', 0),
    (100, 1, NULL, NULL, 'dev-openid-student-a', '同学甲', 'STUDENT', 'ACTIVE', 'APPROVED', 0),
    (101, 1, NULL, NULL, 'dev-openid-student-b', '同学乙', 'STUDENT', 'ACTIVE', 'APPROVED', 0),
    (102, 1, NULL, NULL, 'dev-openid-unverified', '待认证同学', 'STUDENT', 'ACTIVE', 'UNVERIFIED', 0);

INSERT INTO campus_identity_binding
    (id, user_id, campus_id, identifier_type, identifier_fingerprint, identifier_masked, status, reviewer_id, verified_at)
VALUES
    (1000, 100, 1, 'STUDENT_NO', REPEAT('a', 64), '******01', 'APPROVED', 2, CURRENT_TIMESTAMP(3)),
    (1001, 101, 1, 'STUDENT_NO', REPEAT('b', 64), '******02', 'APPROVED', 2, CURRENT_TIMESTAMP(3));

INSERT INTO buddy_activity
    (id, campus_id, creator_id, scene_name, title, description, meeting_mode, public_location,
     member_location_detail, join_requirement, join_questions_json, start_at, end_at, apply_deadline,
     capacity, accepted_count, review_status, lifecycle_status, moderation_status, reviewer_id, version)
VALUES
    (2000, 1, 100, '自定义场景', '周末校园纪录片拍摄搭子',
     '一起完成校园纪录片的分镜讨论和实地拍摄，欢迎对摄影或采访感兴趣的同学。',
     'OFFLINE', '图书馆附近', '图书馆正门集合', '守时并尊重被采访者隐私',
     JSON_ARRAY('是否有摄影经验？'), DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY),
     DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY) + INTERVAL 3 HOUR,
     DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 5 DAY), 4, 0, 'APPROVED', 'RECRUITING', 'NORMAL', 2, 0);

INSERT INTO buddy_member (id, activity_id, user_id, member_role, status, completion_status)
VALUES (3000, 2000, 100, 'CREATOR', 'ACTIVE', 'PENDING');

INSERT INTO conversation (id, activity_id, status) VALUES (4000, 2000, 'OPEN');
