package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.test.context.ActiveProfiles;
import javax.imageio.ImageIO;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoreFlowIntegrationTest {
    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    @Test
    @Order(1)
    void publishReviewApplyAndAcceptWithPermissionChecks() {
        assertThat(rest.getForEntity("/api/v1/activities", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/v1/auth/admin-login",
                Map.of("username", "admin", "password", "WrongPass123!"), null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String applicant = wechatToken("student-b");

        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(2), creator));
        long activityId = number(activity, "id");
        assertThat(activity.get("reviewStatus")).isEqualTo("NOT_SUBMITTED");
        assertThat(activity.get("lifecycleStatus")).isEqualTo("DRAFT");

        ResponseEntity<Map> forbidden = exchange("/api/v1/activities/" + activityId, HttpMethod.PUT,
                Map.of("version", 0, "activity", activityInput(2)), applicant);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        assertThat(submitted.get("reviewStatus")).isEqualTo("PENDING");

        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        Map<String, Object> approved = data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));
        assertThat(approved.get("reviewStatus")).isEqualTo("APPROVED");
        assertThat(approved.get("lifecycleStatus")).isEqualTo("RECRUITING");

        Map<String, Object> publicDetail = data(get("/api/v1/activities/" + activityId, applicant));
        assertThat(publicDetail).doesNotContainKey("memberLocationDetail");

        Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("answers", List.of("有手机外接麦克风"), "message", "愿意参与整理素材"), applicant));
        Map<String, Object> accepted = data(post("/api/v1/applications/" + number(application, "id") + "/decision",
                Map.of("version", number(application, "version"), "accept", true), creator));
        assertThat(accepted.get("status")).isEqualTo("ACCEPTED");

        Map<String, Object> memberDetail = data(get("/api/v1/activities/" + activityId, applicant));
        assertThat(memberDetail.get("memberLocationDetail")).isEqualTo("校史馆东门长椅集合");
        assertThat(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "重复申请"), applicant).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Integer acceptedCount = jdbc.queryForObject(
                "SELECT accepted_count FROM buddy_activity WHERE id = ?", Integer.class, activityId);
        Integer memberCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM buddy_member WHERE activity_id = ? AND member_role = 'PARTICIPANT' AND status = 'ACTIVE'",
                Integer.class, activityId);
        Integer conversationCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM conversation WHERE activity_id = ?", Integer.class, activityId);
        assertThat(List.of(acceptedCount, memberCount, conversationCount)).containsExactly(1, 1, 1);
    }

    @Test
    @Order(2)
    void concurrentAcceptanceNeverExceedsCapacity() throws Exception {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(2), creator));
        long activityId = number(activity, "id");
        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));

        List<String> applicantTokens = new ArrayList<>();
        List<Long> applicationIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            long userId = 5000L + i;
            String code = "capacity-" + i;
            jdbc.update("""
                    INSERT INTO sys_user
                        (id, campus_id, wechat_openid, nickname, role, status, verification_status, token_version)
                    VALUES (?, 1, ?, ?, 'STUDENT', 'ACTIVE', 'APPROVED', 0)
                    ON DUPLICATE KEY UPDATE campus_id = 1, status = 'ACTIVE',
                        verification_status = 'APPROVED', token_version = 0
                    """, userId, "dev-openid-" + code, "并发同学" + i);
            String token = wechatToken(code);
            applicantTokens.add(token);
            Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                    Map.of("message", "并发名额测试"), token));
            applicationIds.add(number(application, "id"));
        }

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpStatus>> futures = new ArrayList<>();
        try {
            for (long applicationId : applicationIds) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    return HttpStatus.valueOf(post("/api/v1/applications/" + applicationId + "/decision",
                            Map.of("version", 0, "accept", true), creator).getStatusCode().value());
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            long successes = 0;
            long conflicts = 0;
            for (Future<HttpStatus> future : futures) {
                HttpStatus status = future.get(20, TimeUnit.SECONDS);
                if (status == HttpStatus.OK) successes++;
                if (status == HttpStatus.CONFLICT) conflicts++;
            }
            assertThat(successes).isEqualTo(2);
            assertThat(conflicts).isEqualTo(18);
        } finally {
            pool.shutdownNow();
        }

        Integer acceptedCount = jdbc.queryForObject(
                "SELECT accepted_count FROM buddy_activity WHERE id = ?", Integer.class, activityId);
        Integer memberCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM buddy_member WHERE activity_id = ? AND member_role = 'PARTICIPANT' AND status = 'ACTIVE'",
                Integer.class, activityId);
        assertThat(acceptedCount).isEqualTo(2);
        assertThat(memberCount).isEqualTo(2);
    }

    @Test
    @Order(3)
    void suspendedStudentCanOnlyAppealAndRevocationRestoresAccount() {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String admin = adminToken("admin", "Admin123!");
        String reporter = wechatToken("student-a");
        String targetOldToken = wechatToken("student-b");

        Map<String, Object> report = data(post("/api/v1/reports", Map.of(
                "targetType", "USER",
                "targetId", 101,
                "reasonCode", "HARASSMENT",
                "description", "测试账号治理与申诉闭环"), reporter));
        Map<String, Object> claimed = data(post("/api/v1/review/reports/" + number(report, "id") + "/claim",
                Map.of("version", number(report, "version")), reviewer));
        @SuppressWarnings("unchecked")
        Map<String, Object> claimedReport = (Map<String, Object>) claimed.get("report");
        Map<String, Object> actioned = data(post("/api/v1/review/reports/" + number(report, "id") + "/decision",
                Map.of("version", number(claimedReport, "version"), "actioned", true,
                        "actionType", "SUSPEND_USER", "resolution", "确认违规，暂停账号", "durationHours", 24),
                reviewer));
        @SuppressWarnings("unchecked")
        Map<String, Object> actionedReport = (Map<String, Object>) actioned.get("report");
        assertThat(actionedReport.get("status")).isEqualTo("ACTIONED");

        assertThat(get("/api/v1/me", targetOldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String restrictedToken = wechatToken("student-b");
        assertThat(post("/api/v1/activities", activityInput(2), restrictedToken).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        ResponseEntity<Map> affected = get("/api/v1/reports/affected", restrictedToken);
        assertThat(affected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(affected.getBody().toString()).doesNotContain("reporterId");

        Map<String, Object> appealed = data(post("/api/v1/reports/" + number(report, "id") + "/appeal",
                Map.of("version", number(actionedReport, "version"), "reason", "请求平台复核，本次处置有误"),
                restrictedToken));
        assertThat(appealed.get("status")).isEqualTo("APPEALED");
        Map<String, Object> revoked = data(post("/api/v1/admin/reports/" + number(report, "id") + "/appeal-decision",
                Map.of("version", number(appealed, "version"), "uphold", false,
                        "resolution", "复核后撤销原账号处置"), admin));
        @SuppressWarnings("unchecked")
        Map<String, Object> revokedReport = (Map<String, Object>) revoked.get("report");
        assertThat(revokedReport.get("status")).isEqualTo("REVOKED");

        assertThat(get("/api/v1/me", restrictedToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String restoredToken = wechatToken("student-b");
        assertThat(get("/api/v1/me", restoredToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT status FROM sys_user WHERE id = 101", String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM moderation_action WHERE report_id = ?", Integer.class,
                number(report, "id"))).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE target_type = 'REPORT' AND target_id = ?",
                Integer.class, String.valueOf(number(report, "id")))).isGreaterThanOrEqualTo(4);
    }

    @Test
    @Order(4)
    void imageUploadModerationBindingAndIdentityProofAreEnforced() throws Exception {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String otherStudent = wechatToken("student-b");
        byte[] png = pngBytes();

        Map<String, Object> image = data(upload(png, "campus-map.png", "ACTIVITY_IMAGE", null, creator));
        long imageId = number(image, "id");
        assertThat(image.get("status")).isEqualTo("PENDING_SCAN");
        assertThat(get("/api/v1/files/" + imageId + "/url", creator).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/v1/files/" + imageId + "/url", otherStudent).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<String, Object> approvedFile = data(post("/api/v1/review/files/" + imageId + "/decision",
                Map.of("approve", true), reviewer));
        assertThat(approvedFile.get("status")).isEqualTo("APPROVED");

        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(2), creator));
        long activityId = number(activity, "id");
        ResponseEntity<Map> attached = exchange("/api/v1/activities/" + activityId + "/media", HttpMethod.PUT,
                Map.of("fileIds", List.of(imageId)), creator);
        assertThat(attached.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> ownerDetail = data(get("/api/v1/activities/" + activityId, creator));
        List<?> mediaIds = (List<?>) ownerDetail.get("mediaIds");
        assertThat(mediaIds).hasSize(1);
        assertThat(Long.parseLong(String.valueOf(mediaIds.get(0)))).isEqualTo(imageId);
        assertThat(get("/api/v1/files/" + imageId + "/url", otherStudent).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange("/api/v1/files/" + imageId, HttpMethod.DELETE, null, creator).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));
        Map<String, Object> publicUrl = data(get("/api/v1/files/" + imageId + "/url", otherStudent));
        assertThat(publicUrl.get("url")).asString().startsWith("memory://");

        byte[] fakeSvg = "<svg xmlns='http://www.w3.org/2000/svg'><script/></svg>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(upload(fakeSvg, "attack.svg", "IDENTITY_PROOF", 1L, otherStudent).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        String unverified = wechatToken("identity-new");
        Map<String, Object> proof = data(upload(png, "student-card.png", "IDENTITY_PROOF", 1L, unverified));
        long proofId = number(proof, "id");
        Map<String, Object> binding = data(post("/api/v1/me/identity-bindings", Map.of(
                "campusId", 1,
                "identifierType", "STUDENT_NO",
                "identifier", "2026009999",
                "proofFileId", proofId), unverified));
        assertThat(binding.get("status")).isEqualTo("PENDING");
        data(post("/api/v1/review/files/" + proofId + "/decision",
                Map.of("approve", true), reviewer));
        data(post("/api/v1/review/identity-bindings/" + number(binding, "id") + "/decision", Map.of(
                "version", number(binding, "version"), "approved", true, "reason", "材料核验通过"), reviewer));
        assertThat(get("/api/v1/me", unverified).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String verified = wechatToken("identity-new");
        Map<String, Object> verifiedProfile = data(get("/api/v1/me", verified));
        assertThat(verifiedProfile.get("verificationStatus")).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT business_id FROM file_object WHERE id = ?", Long.class, proofId))
                .isEqualTo(number(binding, "id"));
    }

    @Test
    @Order(5)
    void websocketUsesSingleUseTicketPersistsBeforeAckAndSupportsCursorPull() throws Exception {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String applicant = wechatToken("student-b");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(2), creator));
        long activityId = number(activity, "id");
        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));
        Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "WebSocket 集成测试"), applicant));
        data(post("/api/v1/applications/" + number(application, "id") + "/decision",
                Map.of("version", number(application, "version"), "accept", true), creator));
        long conversationId = jdbc.queryForObject(
                "SELECT id FROM conversation WHERE activity_id = ?", Long.class, activityId);

        String creatorTicket = (String) data(post("/api/v1/ws-ticket", Map.of(), creator)).get("ticket");
        String applicantTicket = (String) data(post("/api/v1/ws-ticket", Map.of(), applicant)).get("ticket");
        WsListener creatorListener = new WsListener();
        WsListener applicantListener = new WsListener();
        WebSocket creatorSocket = openSocket(creatorTicket, creatorListener);
        WebSocket applicantSocket = openSocket(applicantTicket, applicantListener);
        assertThat(creatorListener.await("\"type\":\"CONNECTED\"", Duration.ofSeconds(5))).isNotNull();
        assertThat(applicantListener.await("\"type\":\"CONNECTED\"", Duration.ofSeconds(5))).isNotNull();

        String clientMessageId = "ws-test-message-1";
        String payload = "{\"type\":\"SEND_MESSAGE\",\"conversationId\":" + conversationId
                + ",\"clientMessageId\":\"" + clientMessageId
                + "\",\"messageType\":\"TEXT\",\"content\":\"大家好，准备开始活动\"}";
        applicantSocket.sendText(payload, true).join();
        String ack = applicantListener.await("\"type\":\"ACK\"", Duration.ofSeconds(5));
        String pushed = creatorListener.await("\"type\":\"MESSAGE\"", Duration.ofSeconds(5));
        assertThat(ack).contains("\"duplicate\":false");
        assertThat(pushed).contains("大家好，准备开始活动");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE conversation_id = ? AND client_message_id = ?",
                Integer.class, conversationId, clientMessageId)).isEqualTo(1);

        applicantSocket.sendText(payload, true).join();
        String duplicateAck = applicantListener.await("\"duplicate\":true", Duration.ofSeconds(5));
        assertThat(duplicateAck).contains("\"type\":\"ACK\"");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE conversation_id = ? AND client_message_id = ?",
                Integer.class, conversationId, clientMessageId)).isEqualTo(1);

        ResponseEntity<Map> historyRaw = get("/api/v1/conversations/" + conversationId + "/messages?afterId=0&limit=20", applicant);
        assertThat(historyRaw.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) historyRaw.getBody().get("data")).hasSize(1);

        boolean reusedTicketRejected = false;
        try {
            openSocket(applicantTicket, new WsListener());
        } catch (Exception ex) {
            reusedTicketRejected = true;
        }
        assertThat(reusedTicketRejected).isTrue();

        jdbc.update("""
                INSERT IGNORE INTO sys_user
                    (id, campus_id, wechat_openid, nickname, role, status, verification_status, token_version)
                VALUES (160, 1, 'dev-openid-ws-outsider', '会话外同学', 'STUDENT', 'ACTIVE', 'APPROVED', 0)
                """);
        String outsider = wechatToken("ws-outsider");
        String outsiderTicket = (String) data(post("/api/v1/ws-ticket", Map.of(), outsider)).get("ticket");
        WsListener outsiderListener = new WsListener();
        WebSocket outsiderSocket = openSocket(outsiderTicket, outsiderListener);
        outsiderListener.await("\"type\":\"CONNECTED\"", Duration.ofSeconds(5));
        outsiderSocket.sendText(payload.replace(clientMessageId, "outsider-message"), true).join();
        assertThat(outsiderListener.await("\"type\":\"ERROR\"", Duration.ofSeconds(5)))
                .contains("FORBIDDEN");

        String ticketIssuedBeforeLogout = (String) data(post("/api/v1/ws-ticket", Map.of(), applicant)).get("ticket");
        assertThat(post("/api/v1/auth/logout", Map.of(), applicant).getStatusCode()).isEqualTo(HttpStatus.OK);
        applicantSocket.sendText(payload.replace(clientMessageId, "message-after-logout"), true).join();
        assertThat(applicantListener.await("\"type\":\"ERROR\"", Duration.ofSeconds(5)))
                .contains("UNAUTHENTICATED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message WHERE conversation_id = ? AND client_message_id = ?",
                Integer.class, conversationId, "message-after-logout")).isZero();

        boolean ticketIssuedBeforeLogoutRejected = false;
        try {
            openSocket(ticketIssuedBeforeLogout, new WsListener());
        } catch (Exception ex) {
            ticketIssuedBeforeLogoutRejected = true;
        }
        assertThat(ticketIssuedBeforeLogoutRejected).isTrue();

        creatorSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        outsiderSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
    }

    @Test
    @Order(6)
    void blockHidesActivityAndIneligibleApplicantCannotBeAccepted() {
        String reviewer = adminToken("reviewer", "Reviewer123!");
        String creator = wechatToken("student-a");
        String applicant = wechatToken("student-b");
        Map<String, Object> activity = data(post("/api/v1/activities", activityInput(2), creator));
        long activityId = number(activity, "id");
        Map<String, Object> submitted = data(post("/api/v1/activities/" + activityId + "/submit",
                Map.of("version", number(activity, "version")), creator));
        Map<String, Object> claimed = data(post("/api/v1/review/activities/" + activityId + "/claim",
                Map.of("version", number(submitted, "version")), reviewer));
        data(post("/api/v1/review/activities/" + activityId + "/decision",
                Map.of("version", number(claimed, "version"), "approve", true), reviewer));

        assertThat(post("/api/v1/blocks/101", Map.of(), creator).getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> discover = data(get("/api/v1/activities?page=1&size=50", applicant));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) discover.get("records");
        assertThat(records).noneMatch(item -> number(item, "id") == activityId);
        assertThat(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "拉黑后不应创建申请"), applicant).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(exchange("/api/v1/blocks/101", HttpMethod.DELETE, null, creator).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        Map<String, Object> application = data(post("/api/v1/activities/" + activityId + "/applications",
                Map.of("message", "恢复后正常申请"), applicant));

        try {
            jdbc.update("UPDATE sys_user SET status = 'SUSPENDED', token_version = token_version + 1 WHERE id = 101");
            assertThat(post("/api/v1/applications/" + number(application, "id") + "/decision",
                    Map.of("version", number(application, "version"), "accept", true), creator).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
            assertThat(jdbc.queryForObject("SELECT status FROM buddy_application WHERE id = ?", String.class,
                    number(application, "id"))).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("SELECT accepted_count FROM buddy_activity WHERE id = ?", Integer.class,
                    activityId)).isZero();
        } finally {
            jdbc.update("UPDATE sys_user SET status = 'ACTIVE', token_version = token_version + 1 WHERE id = 101");
        }

        try {
            jdbc.update("UPDATE sys_user SET status = 'LIMITED', token_version = token_version + 1 WHERE id = 100");
            String limitedCreator = wechatToken("student-a");
            assertThat(post("/api/v1/applications/" + number(application, "id") + "/decision",
                    Map.of("version", number(application, "version"), "accept", false), limitedCreator).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        } finally {
            jdbc.update("UPDATE sys_user SET status = 'ACTIVE', token_version = token_version + 1 WHERE id = 100");
        }
    }

    @Test
    @Order(7)
    void localAdminOriginPassesCorsPreflight() {
        HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://127.0.0.1:5174");
        headers.setAccessControlRequestMethod(HttpMethod.POST);
        headers.setAccessControlRequestHeaders(List.of("authorization", "content-type"));

        ResponseEntity<Void> response = rest.exchange(
                "/api/v1/auth/admin-login", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://127.0.0.1:5174");
    }

    private Map<String, Object> activityInput(int capacity) {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("sceneName", "校园创意实践");
        input.put("title", "一起完成校园声音地图采集");
        input.put("description", "在校园内分组采集环境声音并整理地点故事，最终共同制作一份校园声音地图。");
        input.put("meetingMode", "OFFLINE");
        input.put("publicLocation", "校史馆附近");
        input.put("memberLocationDetail", "校史馆东门长椅集合");
        input.put("joinRequirement", "守时并尊重录音对象隐私");
        input.put("joinQuestions", List.of("是否有录音设备？"));
        input.put("startAt", start.toString());
        input.put("endAt", start.plus(3, ChronoUnit.HOURS).toString());
        input.put("applyDeadline", start.minus(1, ChronoUnit.DAYS).toString());
        input.put("capacity", capacity);
        input.put("tags", List.of("声音采集", "校园创作"));
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

    private ResponseEntity<Map> upload(byte[] bytes, String filename, String businessType,
                                       Long campusId, String token) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return filename; }
        };
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", resource);
        parts.add("businessType", businessType);
        if (campusId != null) parts.add("campusId", campusId.toString());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        return rest.exchange("/api/v1/files", HttpMethod.POST, new HttpEntity<>(parts, headers), Map.class);
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x3366cc);
        image.setRGB(1, 0, 0xf4b942);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, "png", output)).isTrue();
        return output.toByteArray();
    }

    private WebSocket openSocket(String ticket, WsListener listener) throws Exception {
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/ws?ticket=" + ticket), listener)
                .get(8, TimeUnit.SECONDS);
    }

    private static final class WsListener implements WebSocket.Listener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        String await(String fragment, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                long remaining = deadline - System.nanoTime();
                String value = messages.poll(Math.max(1, remaining), TimeUnit.NANOSECONDS);
                if (value == null) return null;
                if (value.contains(fragment)) return value;
            }
            return null;
        }
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
