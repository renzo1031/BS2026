package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;

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
class PlatformAdminManagementIntegrationTest {
    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void platformAdminManagesCampusUsersAndTagsWithAuditTrail() {
        String admin = adminToken("admin", "Admin123!");
        String reviewer = adminToken("reviewer", "Reviewer123!");
        assertThat(get("/api/v1/admin/users", reviewer).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        String code = "QA" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> campus = data(post("/api/v1/admin/campuses",
                Map.of("name", "质量学院", "code", code, "status", "ACTIVE", "identityLabel", "校园号"), admin));
        long campusId = number(campus, "id");
        Map<String, Object> updatedCampus = data(exchange("/api/v1/admin/campuses/" + campusId, HttpMethod.PUT,
                Map.of("name", "质量工程学院", "code", code, "status", "ACTIVE", "identityLabel", "学工号"), admin));
        assertThat(updatedCampus.get("identityLabel")).isEqualTo("学工号");

        String oldStudentToken = wechatToken("management-user");
        @SuppressWarnings("unchecked")
        Map<String, Object> loginUser = (Map<String, Object>) data(post("/api/v1/auth/wechat-login",
                Map.of("code", "management-user"), null)).get("user");
        long userId = number(loginUser, "id");
        Map<String, Object> managed = data(exchange("/api/v1/admin/users/" + userId, HttpMethod.PUT,
                Map.of("nickname", "校级审核员甲", "campusId", campusId,
                        "role", "CAMPUS_REVIEWER", "status", "ACTIVE"), admin));
        assertThat(managed.get("role")).isEqualTo("CAMPUS_REVIEWER");
        assertThat(managed.get("campusName")).isEqualTo("质量工程学院");
        assertThat(get("/api/v1/me", oldStudentToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Map<String, Object> users = data(get("/api/v1/admin/users?keyword=校级审核员甲", admin));
        assertThat((List<?>) users.get("records")).hasSize(1);

        Map<String, Object> tag = data(post("/api/v1/admin/tags",
                Map.of("campusId", campusId, "name", "实验招募", "status", "ACTIVE"), admin));
        Map<String, Object> inactiveTag = data(exchange("/api/v1/admin/tags/" + number(tag, "id"), HttpMethod.PUT,
                Map.of("name", "实验共创", "status", "INACTIVE"), admin));
        assertThat(inactiveTag.get("status")).isEqualTo("INACTIVE");

        Map<String, Object> tags = data(get("/api/v1/admin/tags?campusId=" + campusId, admin));
        assertThat((List<?>) tags.get("records")).hasSize(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM audit_log
                 WHERE action_name IN ('ADMIN_CAMPUS_CREATE', 'ADMIN_CAMPUS_UPDATE',
                    'ADMIN_USER_UPDATE', 'ADMIN_TAG_CREATE', 'ADMIN_TAG_UPDATE')
                """, Integer.class)).isGreaterThanOrEqualTo(5);
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
}
