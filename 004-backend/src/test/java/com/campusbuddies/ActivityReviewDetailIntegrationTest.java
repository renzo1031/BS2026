package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivityReviewDetailIntegrationTest {
    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void detailIsVisibleToSameCampusAndPlatformReviewersOnly() {
        String creator = wechatToken("student-a");
        String campusReviewer = adminToken("reviewer", "Reviewer123!");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(), creator));
        long activityId = number(activity, "id");
        data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));

        Map<String, Object> sameCampus = data(get("/api/v1/review/activities/" + activityId, campusReviewer));
        @SuppressWarnings("unchecked")
        Map<String, Object> creatorView = (Map<String, Object>) sameCampus.get("creator");
        assertThat(creatorView.get("nickname")).isEqualTo("同学甲");
        assertThat(sameCampus).containsKeys("activity", "applications", "participants", "media", "timeline");
        @SuppressWarnings("unchecked")
        Map<String, Object> activityView = (Map<String, Object>) sameCampus.get("activity");
        assertThat(activityView).containsKeys("lifecycleStatus", "moderationStatus", "updatedAt");
        assertThat(activityView.get("updatedAt")).isNotNull();
        assertThat((List<?>) sameCampus.get("timeline")).isNotEmpty();
        assertThat(sameCampus.toString()).doesNotContain("wechatOpenid", "passwordHash", "identifierPlaintext");

        seedCrossCampusReviewer();
        String crossCampusReviewer = adminToken("detail-cross-reviewer", "Reviewer123!");
        assertThat(get("/api/v1/review/activities/" + activityId, crossCampusReviewer).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        String platformAdmin = adminToken("admin", "Admin123!");
        assertThat(get("/api/v1/review/activities/" + activityId, platformAdmin).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void activityManagementListIncludesAllStatesWithinReviewerCampusScope() {
        String creator = wechatToken("student-a");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(), creator));
        long activityId = number(activity, "id");

        String campusReviewer = adminToken("reviewer", "Reviewer123!");
        Map<String, Object> campusPage = data(get(
                "/api/v1/operations/activities?page=1&size=20&reviewStatus=NOT_SUBMITTED&keyword=审核详情",
                campusReviewer));
        assertThat(recordIds(campusPage)).contains(activityId);

        seedCrossCampusReviewer();
        String crossCampusReviewer = adminToken("detail-cross-reviewer", "Reviewer123!");
        Map<String, Object> crossCampusPage = data(get(
                "/api/v1/operations/activities?page=1&size=20&reviewStatus=NOT_SUBMITTED&keyword=审核详情",
                crossCampusReviewer));
        assertThat(recordIds(crossCampusPage)).doesNotContain(activityId);

        String platformAdmin = adminToken("admin", "Admin123!");
        Map<String, Object> platformPage = data(get(
                "/api/v1/operations/activities?page=1&size=20&reviewStatus=NOT_SUBMITTED&keyword=审核详情",
                platformAdmin));
        assertThat(recordIds(platformPage)).contains(activityId);

        assertThat(get("/api/v1/operations/activities?page=1&size=20", creator).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void activityDetailIncludesGovernanceTimelineForReportedActivity() {
        String creator = wechatToken("student-a");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(), creator));
        long activityId = number(activity, "id");
        long reportId = IdWorker.getId();
        long actionId = IdWorker.getId();
        long auditId = IdWorker.getId();
        jdbc.update("""
                INSERT INTO report_case
                    (id, campus_id, reporter_id, target_type, target_id, reason_code, description, status, version)
                VALUES (?, 1, 1, 'ACTIVITY', ?, 'SAFETY_RISK', '治理链测试', 'ACTIONED', 0)
                """, reportId, activityId);
        jdbc.update("""
                INSERT INTO moderation_action
                    (id, report_id, operator_id, target_type, target_id, action_type, reason)
                VALUES (?, ?, 1, 'ACTIVITY', ?, 'REMOVE_ACTIVITY', '安全风险处置')
                """, actionId, reportId, activityId);
        jdbc.update("""
                INSERT INTO audit_log
                    (id, operator_id, operator_role, campus_id, action_name, target_type, target_id,
                     before_state, after_state, reason)
                VALUES (?, 1, 'PLATFORM_ADMIN', 1, 'ACTION_REPORT', 'REPORT', ?,
                        '{"status":"REVIEWING"}', '{"status":"ACTIONED"}', '安全风险处置')
                """, auditId, String.valueOf(reportId));

        Map<String, Object> detail = data(get("/api/v1/operations/activities/" + activityId,
                adminToken("admin", "Admin123!")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> timeline = (List<Map<String, Object>>) detail.get("timeline");
        assertThat(timeline).extracting(item -> item.get("actionName"))
                .contains("ACTION_REPORT", "REMOVE_ACTIVITY");
    }

    private void seedCrossCampusReviewer() {
        jdbc.update("""
                INSERT INTO campus (id, name, code, status, identity_label)
                VALUES (9202, '详情测试校园', ?, 'ACTIVE', '学号')
                ON DUPLICATE KEY UPDATE status = 'ACTIVE'
                """, "DETAIL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        jdbc.update("""
                INSERT INTO sys_user
                    (id, campus_id, username, password_hash, nickname, role, status, verification_status, token_version)
                VALUES (9200, 9202, 'detail-cross-reviewer',
                    '$2b$12$ZQHoTsOzhaPspkqZLtXMierETlgGeFPqK6pN0WVblI/S4qgXMGnOS',
                    '跨校园审核员', 'CAMPUS_REVIEWER', 'ACTIVE', 'APPROVED', 0)
                ON DUPLICATE KEY UPDATE campus_id = 9202, password_hash = VALUES(password_hash),
                    role = 'CAMPUS_REVIEWER', status = 'ACTIVE', verification_status = 'APPROVED', token_version = 0
                """);
    }

    private Map<String, Object> activityInput() {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("sceneName", "校园创意实践");
        input.put("title", "审核详情读取测试活动");
        input.put("description", "用于验证活动审核详情读取、时间线和校园数据范围校验。审核完成后不影响正常业务流程。");
        input.put("meetingMode", "OFFLINE");
        input.put("publicLocation", "校史馆附近");
        input.put("memberLocationDetail", "校史馆东门集合");
        input.put("joinRequirement", "遵守活动秩序");
        input.put("joinQuestions", List.of("是否愿意参与？"));
        input.put("startAt", start.toString());
        input.put("endAt", start.plus(3, ChronoUnit.HOURS).toString());
        input.put("applyDeadline", start.minus(1, ChronoUnit.DAYS).toString());
        input.put("capacity", 4);
        input.put("tags", List.of("审核测试"));
        return input;
    }

    private String adminToken(String username, String password) {
        return (String) data(post("/api/v1/auth/admin-login", Map.of("username", username, "password", password), null))
                .get("accessToken");
    }

    private String wechatToken(String code) {
        return (String) data(post("/api/v1/auth/wechat-login", Map.of("code", code), null)).get("accessToken");
    }

    private ResponseEntity<Map> get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token);
    }

    private ResponseEntity<Map> post(String path, Object body, String token) {
        return exchange(path, HttpMethod.POST, body, token);
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return (Map<String, Object>) response.getBody().get("data");
    }

    private long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<Long> recordIds(Map<String, Object> page) {
        return ((List<Map<String, Object>>) page.get("records")).stream()
                .map(record -> number(record, "id"))
                .toList();
    }
}
