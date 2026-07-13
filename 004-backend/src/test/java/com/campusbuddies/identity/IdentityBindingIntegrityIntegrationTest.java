package com.campusbuddies.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.file.FileBusinessType;
import com.campusbuddies.file.FileService;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class IdentityBindingIntegrityIntegrationTest {
    private static final long USER_ID = 9_100L;
    private static final long SECOND_CAMPUS_ID = 91L;
    private static final long FIRST_PROOF_ID = 91_001L;
    private static final long SECOND_PROOF_ID = 91_002L;
    private static final long THIRD_PROOF_ID = 91_003L;

    @Autowired private IdentityService service;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private FileService files;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        jdbc.update("INSERT INTO campus (id, name, code, status, identity_label) VALUES (?, ?, ?, 'ACTIVE', '学号')",
                SECOND_CAMPUS_ID, "并发测试大学", "IDENTITY_GUARD_TEST");
        jdbc.update("""
                INSERT INTO sys_user
                    (id, wechat_openid, nickname, role, status, verification_status, token_version)
                VALUES (?, ?, '身份并发测试用户', 'STUDENT', 'ACTIVE', 'UNVERIFIED', 0)
                """, USER_ID, "identity-guard-openid");
        insertProof(FIRST_PROOF_ID, 1L, "first-proof");
        insertProof(SECOND_PROOF_ID, SECOND_CAMPUS_ID, "second-proof");
        insertProof(THIRD_PROOF_ID, 1L, "third-proof");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanFixtures();
    }

    @Test
    void concurrentSubmissionsLeaveExactlyOneCurrentBindingAndCampus() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = pool.submit(() -> submitAfterBarrier(ready, start, 1L,
                    "2026910001", FIRST_PROOF_ID));
            Future<Object> second = pool.submit(() -> submitAfterBarrier(ready, start, SECOND_CAMPUS_ID,
                    "2026910002", SECOND_PROOF_ID));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(IdentityService.View.class::isInstance)).hasSize(1);
            assertThat(results.stream().filter(BusinessException.class::isInstance)
                    .map(BusinessException.class::cast).toList())
                    .singleElement().extracting(BusinessException::errorCode).isEqualTo(ErrorCode.CONFLICT);
        } finally {
            pool.shutdownNow();
        }

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM campus_identity_binding
                 WHERE user_id=? AND status IN ('PENDING','APPROVED')
                """, Integer.class, USER_ID)).isEqualTo(1);
        Long bindingCampus = jdbc.queryForObject("""
                SELECT campus_id FROM campus_identity_binding
                 WHERE user_id=? AND status IN ('PENDING','APPROVED')
                """, Long.class, USER_ID);
        assertThat(jdbc.queryForObject("SELECT campus_id FROM sys_user WHERE id=?", Long.class, USER_ID))
                .isEqualTo(bindingCampus);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO campus_identity_binding
                    (id, user_id, campus_id, identifier_type, identifier_fingerprint,
                     identifier_masked, proof_file_id, status)
                VALUES (91200, ?, 1, 'STUDENT_NO', REPEAT('c', 64), '******0003', ?, 'PENDING')
                """, USER_ID, FIRST_PROOF_ID))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class,
                        error -> assertThat(error.getMostSpecificCause().getMessage())
                                .contains("uk_identity_current_user"));
    }

    @Test
    void rejectedIdentityIsResubmittedOnOriginalRecordWithNewProof() {
        authenticateStudent(null, VerificationStatus.UNVERIFIED, 0);
        IdentityService.View first = service.submit(1L, "STUDENT_NO", "2026910010", FIRST_PROOF_ID);

        authenticateAdmin();
        IdentityService.View rejected = service.decide(first.id(), first.version(), false, "证明信息不清晰");
        assertThat(rejected.status()).isEqualTo(VerificationStatus.REJECTED);

        authenticateStudent(1L, VerificationStatus.REJECTED, 1);
        IdentityService.View resubmitted = service.submit(1L, "STUDENT_NO", "2026910010", THIRD_PROOF_ID);

        assertThat(resubmitted.id()).isEqualTo(first.id());
        assertThat(resubmitted.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(resubmitted.proofFileId()).isEqualTo(THIRD_PROOF_ID);
        assertThat(resubmitted.version()).isEqualTo(rejected.version() + 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campus_identity_binding WHERE user_id=?",
                Integer.class, USER_ID)).isEqualTo(1);
        verify(files).bindEvidence(THIRD_PROOF_ID, USER_ID, 1L,
                FileBusinessType.IDENTITY_PROOF, first.id());
    }

    @Test
    void staleReviewCannotOverwriteNewCurrentCampus() {
        authenticateStudent(null, VerificationStatus.UNVERIFIED, 0);
        IdentityService.View oldBinding = service.submit(1L, "STUDENT_NO", "2026910020", FIRST_PROOF_ID);
        authenticateAdmin();
        service.decide(oldBinding.id(), oldBinding.version(), false, "旧申请驳回");

        authenticateStudent(null, VerificationStatus.REJECTED, 1);
        IdentityService.View current = service.submit(SECOND_CAMPUS_ID, "STUDENT_NO",
                "2026910021", SECOND_PROOF_ID);
        authenticateAdmin();

        assertThatThrownBy(() -> service.decide(oldBinding.id(), oldBinding.version(), true, "过时结论"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(jdbc.queryForObject("SELECT campus_id FROM sys_user WHERE id=?", Long.class, USER_ID))
                .isEqualTo(SECOND_CAMPUS_ID);
        assertThat(jdbc.queryForObject("SELECT status FROM campus_identity_binding WHERE id=?",
                String.class, current.id())).isEqualTo("PENDING");
    }

    @Test
    void approvalRequiresApprovedEvidenceBoundToCurrentBinding() {
        authenticateStudent(null, VerificationStatus.UNVERIFIED, 0);
        IdentityService.View binding = service.submit(1L, "STUDENT_NO", "2026910030", FIRST_PROOF_ID);
        doThrow(new BusinessException(ErrorCode.INVALID_ARGUMENT, "认证证明尚未审核通过"))
                .when(files).requireApprovedEvidence(FIRST_PROOF_ID, USER_ID, 1L,
                        FileBusinessType.IDENTITY_PROOF, binding.id());
        authenticateAdmin();

        assertThatThrownBy(() -> service.decide(binding.id(), binding.version(), true, "同意"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT));
        verify(files).requireApprovedEvidence(FIRST_PROOF_ID, USER_ID, 1L,
                FileBusinessType.IDENTITY_PROOF, binding.id());
        assertThat(jdbc.queryForObject("SELECT status FROM campus_identity_binding WHERE id=?",
                String.class, binding.id())).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT verification_status FROM sys_user WHERE id=?",
                String.class, USER_ID)).isEqualTo("PENDING");
    }

    private Object submitAfterBarrier(CountDownLatch ready, CountDownLatch start, long campusId,
                                      String identifier, long proofFileId) throws InterruptedException {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
            authenticateStudent(null, VerificationStatus.UNVERIFIED, 0);
            return service.submit(campusId, "STUDENT_NO", identifier, proofFileId);
        } catch (BusinessException error) {
            return error;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateStudent(Long campusId, VerificationStatus verificationStatus, int tokenVersion) {
        authenticate(new AuthPrincipal(USER_ID, campusId, UserRole.STUDENT, UserStatus.ACTIVE,
                verificationStatus, tokenVersion, "identity-test-token", Instant.now().plusSeconds(600).getEpochSecond()));
    }

    private void authenticateAdmin() {
        authenticate(new AuthPrincipal(1L, null, UserRole.PLATFORM_ADMIN, UserStatus.ACTIVE,
                VerificationStatus.APPROVED, 0, "identity-admin-token", Instant.now().plusSeconds(600).getEpochSecond()));
    }

    private void authenticate(AuthPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private void insertProof(long id, long campusId, String objectKey) {
        jdbc.update("""
                INSERT INTO file_object
                    (id, owner_id, campus_id, business_type, bucket_name, object_key,
                     content_type, byte_size, sha256, status)
                VALUES (?, ?, ?, 'IDENTITY_PROOF', 'identity-test', ?,
                        'image/png', 16, REPEAT('d', 64), 'APPROVED')
                """, id, USER_ID, campusId, objectKey);
    }

    private void cleanFixtures() {
        jdbc.update("DELETE FROM notification WHERE user_id=?", USER_ID);
        jdbc.update("DELETE FROM campus_identity_binding WHERE user_id=?", USER_ID);
        jdbc.update("DELETE FROM file_object WHERE owner_id=?", USER_ID);
        jdbc.update("DELETE FROM sys_user WHERE id=?", USER_ID);
        jdbc.update("DELETE FROM campus WHERE id=?", SECOND_CAMPUS_ID);
    }
}
