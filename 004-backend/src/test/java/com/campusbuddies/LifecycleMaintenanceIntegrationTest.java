package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusbuddies.maintenance.LifecycleMaintenanceService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LifecycleMaintenanceIntegrationTest {
    @Autowired
    private LifecycleMaintenanceService maintenance;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private org.springframework.boot.test.web.client.TestRestTemplate rest;

    @Test
    void runOnceReleasesExpiredClaimsAutoCompletesTimedOutActivityAndReleasesReportClaim() {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String applicant = wechatToken("student-b");

        Map<String, Object> reviewActivity = data(post("/api/v1/activities", activityInput("认领释放活动", 4,
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.DAYS).minus(2, ChronoUnit.HOURS)), creator));
        long reviewActivityId = number(reviewActivity, "id");
        Map<String, Object> reviewSubmitted = data(post("/api/v1/activities/" + reviewActivityId + "/submit",
                Map.of("version", number(reviewActivity, "version")), creator));
        Map<String, Object> reviewClaimed = data(post("/api/v1/review/activities/" + reviewActivityId + "/claim",
                Map.of("version", number(reviewSubmitted, "version")), reviewer));
        jdbc.update("UPDATE buddy_activity SET claim_expires_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE) WHERE id = ?",
                reviewActivityId);

        Map<String, Object> report = data(post("/api/v1/reports", Map.of(
                "targetType", "USER",
                "targetId", 101,
                "reasonCode", "HARASSMENT",
                "description", "测试举报认领超时释放"), creator));
        long reportId = number(report, "id");
        data(post("/api/v1/review/reports/" + reportId + "/claim",
                Map.of("version", number(report, "version")), reviewer));
        jdbc.update("UPDATE report_case SET claim_expires_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE) WHERE id = ?",
                reportId);

        Map<String, Object> activity = data(post("/api/v1/activities", activityInput("自动完成活动", 2,
                Instant.now().plus(5, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now().plus(4, ChronoUnit.MINUTES)), creator));
        long activityId = number(activity, "id");
        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));
        Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "准备参与超时自动完成测试"), applicant));
        data(post("/api/v1/applications/" + number(application, "id") + "/decision",
                Map.of("version", number(application, "version"), "accept", true), creator));
        data(post("/api/v1/activities/" + activityId + "/start",
                Map.of("version", activityVersion(activityId)), creator));
        data(post("/api/v1/activities/" + activityId + "/request-completion",
                Map.of("version", activityVersion(activityId)), creator));
        jdbc.update("UPDATE buddy_activity SET completion_deadline_at = DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE) WHERE id = ?",
                activityId);

        maintenance.runOnce(Instant.now());

        assertThat(jdbc.queryForObject(
                "SELECT reviewer_id FROM buddy_activity WHERE id = ?", Long.class, reviewActivityId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT claim_expires_at FROM buddy_activity WHERE id = ?", java.sql.Timestamp.class, reviewActivityId))
                .isNull();
        assertThat(jdbc.queryForObject(
                "SELECT status FROM report_case WHERE id = ?", String.class, reportId)).isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject(
                "SELECT assignee_id FROM report_case WHERE id = ?", Long.class, reportId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT claim_expires_at FROM report_case WHERE id = ?", java.sql.Timestamp.class, reportId))
                .isNull();
        assertThat(jdbc.queryForObject(
                "SELECT lifecycle_status FROM buddy_activity WHERE id = ?", String.class, activityId))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM buddy_member WHERE activity_id = ? AND completion_status = 'AUTO_CONFIRMED'",
                Integer.class, activityId)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM conversation WHERE activity_id = ?", String.class, activityId))
                .isEqualTo("READ_ONLY");
        assertThat(jdbc.queryForObject(
                "SELECT lifecycle_status FROM buddy_activity WHERE id = ?", String.class, reviewActivityId))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject(
                "SELECT assignee_id FROM report_case WHERE id = ?", Long.class, reportId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT version FROM buddy_activity WHERE id = ?", Integer.class, reviewActivityId)).isGreaterThan(0);
    }

    @Test
    void disputedCompletionCreatesCompletionDisputeReport() {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String applicant = wechatToken("student-b");

        Map<String, Object> activity = data(post("/api/v1/activities", activityInput("完成争议活动", 2,
                Instant.now().plus(10, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now().plus(8, ChronoUnit.MINUTES)), creator));
        long activityId = number(activity, "id");
        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));
        Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "完成争议测试申请"), applicant));
        data(post("/api/v1/applications/" + number(application, "id") + "/decision",
                Map.of("version", number(application, "version"), "accept", true), creator));
        data(post("/api/v1/activities/" + activityId + "/start",
                Map.of("version", activityVersion(activityId)), creator));
        data(post("/api/v1/activities/" + activityId + "/request-completion",
                Map.of("version", activityVersion(activityId)), creator));

        Map<String, Object> disputed = data(post("/api/v1/activities/" + activityId + "/completion-confirmation",
                Map.of("disputed", true), creator));

        long reportId = jdbc.queryForObject("""
                SELECT id FROM report_case
                 WHERE target_type = 'ACTIVITY' AND target_id = ? AND reason_code = 'COMPLETION_DISPUTE'
                """, Long.class, activityId);
        assertThat(reportId).isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM report_case WHERE id = ?", String.class, reportId))
                .isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("SELECT reporter_id FROM report_case WHERE id = ?", Long.class, reportId))
                .isEqualTo(100L);
        assertThat(jdbc.queryForObject("SELECT completion_status FROM buddy_member WHERE activity_id = ? AND user_id = 100",
                String.class, activityId)).isEqualTo("DISPUTED");
        assertThat(disputed.get("lifecycleStatus")).isEqualTo("COMPLETION_PENDING");
    }

    private Map<String, Object> activityInput(String sceneName, int capacity, Instant startAt,
                                              Instant endAt, Instant applyDeadline) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("sceneName", sceneName);
        input.put("title", sceneName + "搭子招募");
        input.put("description", "用于验证定时维护逻辑的测试活动，包含报名、开始、完成确认和审批闭环。");
        input.put("meetingMode", "OFFLINE");
        input.put("publicLocation", "校门口");
        input.put("memberLocationDetail", "校门口东侧长椅集合");
        input.put("joinRequirement", "按时到场");
        input.put("joinQuestions", List.of("是否能准时参加？"));
        input.put("startAt", startAt.toString());
        input.put("endAt", endAt.toString());
        input.put("applyDeadline", applyDeadline.toString());
        input.put("capacity", capacity);
        input.put("tags", List.of("测试"));
        return input;
    }

    private String adminToken(String username, String password) {
        Map<String, Object> response = data(post("/api/v1/auth/admin-login",
                Map.of("username", username, "password", password), null));
        return (String) response.get("accessToken");
    }

    private String wechatToken(String code) {
        Map<String, Object> response = data(post("/api/v1/auth/wechat-login", Map.of("code", code), null));
        return (String) response.get("accessToken");
    }

    private ResponseEntity<Map> post(String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) headers.setBearerAuth(token);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        return (Map<String, Object>) response.getBody().get("data");
    }

    private long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private int activityVersion(long activityId) {
        return jdbc.queryForObject("SELECT version FROM buddy_activity WHERE id = ?", Integer.class, activityId);
    }
}
