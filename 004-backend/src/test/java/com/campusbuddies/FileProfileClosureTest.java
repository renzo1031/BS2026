package com.campusbuddies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.campus.CampusMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.config.AppProperties;
import com.campusbuddies.file.ActivityMediaMapper;
import com.campusbuddies.file.FileAccessMapper;
import com.campusbuddies.file.FileBusinessType;
import com.campusbuddies.file.FileObject;
import com.campusbuddies.file.FileObjectMapper;
import com.campusbuddies.file.FileService;
import com.campusbuddies.file.FileStatus;
import com.campusbuddies.file.ImageSanitizer;
import com.campusbuddies.file.ObjectStorage;
import com.campusbuddies.notification.NotificationService;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserController;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatsMapper;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class FileProfileClosureTest {
    private FileObjectMapper fileObjects;
    private ObjectStorage storage;
    private NotificationService notifications;
    private FileAccessMapper access;
    private FileService service;

    @BeforeEach
    void setUp() {
        fileObjects = mock(FileObjectMapper.class);
        storage = mock(ObjectStorage.class);
        notifications = mock(NotificationService.class);
        access = mock(FileAccessMapper.class);
        service = new FileService(fileObjects, mock(ActivityMediaMapper.class), mock(BuddyActivityMapper.class),
                mock(CampusMapper.class), storage, mock(ImageSanitizer.class),
                new AppProperties.Minio("http://localhost", "key", "secret", "test-bucket"),
                notifications, access);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanPageAndReadOwnModerationStatus() {
        authenticate(100, 1, UserRole.STUDENT);
        FileObject pending = file(7, 100, 1, FileBusinessType.ACTIVITY_IMAGE, null, FileStatus.PENDING_SCAN);
        Page<FileObject> databasePage = Page.of(1, 10);
        databasePage.setRecords(List.of(pending));
        databasePage.setTotal(1);
        when(fileObjects.selectPage(any(), any())).thenReturn(databasePage);
        when(fileObjects.selectOne(any())).thenReturn(pending);

        PageResult<FileService.FileView> page = service.mine(1, 10);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).singleElement().satisfies(view -> {
            assertThat(view.ownerId()).isEqualTo(100);
            assertThat(view.status()).isEqualTo(FileStatus.PENDING_SCAN);
        });
        assertThat(service.mine(7).status()).isEqualTo(FileStatus.PENDING_SCAN);
    }

    @Test
    void moderationDecisionCreatesInAppNotification() {
        authenticate(2, 1, UserRole.CAMPUS_REVIEWER);
        FileObject pending = file(7, 100, 1, FileBusinessType.AVATAR, null, FileStatus.PENDING_SCAN);
        FileObject approved = file(7, 100, 1, FileBusinessType.AVATAR, null, FileStatus.APPROVED);
        approved.setScanResult("MANUAL_APPROVED");
        when(fileObjects.selectOne(any())).thenReturn(pending, approved);
        when(fileObjects.moderate(7, FileStatus.APPROVED, "MANUAL_APPROVED")).thenReturn(1);

        assertThat(service.moderate(7, true, null).status()).isEqualTo(FileStatus.APPROVED);
        verify(notifications).create(100, "FILE_APPROVED", "图片审核已通过",
                "您上传的图片已通过审核，可以用于对应功能。", "FILE", 7L);
    }

    @Test
    void moderationRejectionKeepsReasonPrefixForClientPolling() {
        authenticate(2, 1, UserRole.CAMPUS_REVIEWER);
        FileObject pending = file(8, 100, 1, FileBusinessType.AVATAR, null, FileStatus.PENDING_SCAN);
        FileObject rejected = file(8, 100, 1, FileBusinessType.AVATAR, null, FileStatus.REJECTED);
        rejected.setScanResult("REJECTED:头像内容不适合作为个人头像");
        when(fileObjects.selectOne(any())).thenReturn(pending, rejected);
        when(fileObjects.moderate(8, FileStatus.REJECTED, "REJECTED:头像内容不适合作为个人头像")).thenReturn(1);

        FileService.FileView result = service.moderate(8, false, "头像内容不适合作为个人头像");

        assertThat(result.status()).isEqualTo(FileStatus.REJECTED);
        assertThat(result.scanResult()).isEqualTo("REJECTED:头像内容不适合作为个人头像");
        verify(notifications).create(100, "FILE_REJECTED", "图片审核未通过",
                "审核原因：头像内容不适合作为个人头像", "FILE", 8L);
    }

    @Test
    void avatarIsBoundOnlyAfterAllOwnershipAndApprovalChecks() {
        FileObject avatar = file(9, 100, 1, FileBusinessType.AVATAR, null, FileStatus.APPROVED);
        when(fileObjects.selectOne(any())).thenReturn(avatar);
        when(fileObjects.bind(9, 100, FileBusinessType.AVATAR, 100, 0)).thenReturn(1);

        service.replaceAvatar(9, 8L, 100, 1L);

        verify(fileObjects).bind(9, 100, FileBusinessType.AVATAR, 100, 0);
        verify(fileObjects).unbindAvatar(8, 100);
    }

    @Test
    void profileViewReturnsSafelyBoundAvatarId() {
        authenticate(100, 1, UserRole.STUDENT);
        SysUserMapper users = mock(SysUserMapper.class);
        UserStatsMapper stats = mock(UserStatsMapper.class);
        FileService files = mock(FileService.class);
        SysUser user = user(100, 1);
        when(users.findByIdForUpdate(100)).thenReturn(user);
        UserController controller = new UserController(users, stats, files);

        UserController.View result = controller.update(new UserController.ProfileInput(
                null, null, null, null, null, 9L)).data();

        verify(files).replaceAvatar(9, null, 100, 1L);
        verify(users).updateById(user);
        assertThat(result.avatarFileId()).isEqualTo(9);
    }

    @Test
    void unverifiedStudentCannotBindAvatar() {
        authenticate(100, 1, UserRole.STUDENT, VerificationStatus.PENDING);
        SysUserMapper users = mock(SysUserMapper.class);
        UserStatsMapper stats = mock(UserStatsMapper.class);
        FileService files = mock(FileService.class);
        SysUser user = user(100, 1);
        user.setVerificationStatus(VerificationStatus.PENDING);
        when(users.findByIdForUpdate(100)).thenReturn(user);
        UserController controller = new UserController(users, stats, files);

        assertThatThrownBy(() -> controller.update(new UserController.ProfileInput(
                null, null, null, null, null, 9L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void unverifiedStudentCannotUploadAvatar() {
        authenticate(100, 1, UserRole.STUDENT, VerificationStatus.PENDING);

        assertThatThrownBy(() -> service.upload(null, FileBusinessType.AVATAR, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void otherStudentCannotReadApprovedButUnboundAvatar() {
        authenticate(101, 1, UserRole.STUDENT);
        FileObject avatar = file(13, 100, 1, FileBusinessType.AVATAR, null, FileStatus.APPROVED);
        when(fileObjects.selectOne(any())).thenReturn(avatar);

        assertThatThrownBy(() -> service.accessUrl(13))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        avatar.setBusinessId(100L);
        when(access.countActiveAvatar(13, 100)).thenReturn(1);
        when(storage.presignedGetUrl(any(), any(), any())).thenReturn("memory://avatar");
        assertThat(service.accessUrl(13).url()).isEqualTo("memory://avatar");
    }

    @Test
    void identityEvidenceMustBeApprovedAndBoundToCurrentBinding() {
        FileObject proof = file(11, 100, 1, FileBusinessType.IDENTITY_PROOF, 501L, FileStatus.PENDING_SCAN);
        when(fileObjects.selectById(11L)).thenReturn(proof);

        assertThatThrownBy(() -> service.requireApprovedEvidence(
                11, 100, 1, FileBusinessType.IDENTITY_PROOF, 501))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT));

        proof.setStatus(FileStatus.APPROVED);
        service.requireApprovedEvidence(11, 100, 1, FileBusinessType.IDENTITY_PROOF, 501);
    }

    @Test
    void chatImageUrlRequiresActiveMembershipInItsActualMessageConversation() {
        authenticate(101, 1, UserRole.STUDENT);
        FileObject image = file(12, 100, 1, FileBusinessType.CHAT_IMAGE, 700L, FileStatus.APPROVED);
        when(fileObjects.selectOne(any())).thenReturn(image);
        when(storage.presignedGetUrl(any(), any(), any())).thenReturn("memory://chat-image");
        when(access.countActiveConversationMember(700, 12, 101)).thenReturn(1);

        assertThat(service.accessUrl(12).url()).isEqualTo("memory://chat-image");

        when(access.countActiveConversationMember(700, 12, 101)).thenReturn(0);
        assertThatThrownBy(() -> service.accessUrl(12))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

        authenticate(100, 1, UserRole.STUDENT);
        when(access.countActiveConversationMember(700, 12, 100)).thenReturn(0);
        assertThatThrownBy(() -> service.accessUrl(12))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void sameCampusReviewerCannotReadChatImageWithoutMembership() {
        authenticate(2, 1, UserRole.CAMPUS_REVIEWER);
        FileObject image = file(14, 100, 1, FileBusinessType.CHAT_IMAGE, 700L, FileStatus.APPROVED);
        when(fileObjects.selectOne(any())).thenReturn(image);
        when(access.countActiveConversationMember(700, 14, 2)).thenReturn(0);

        assertThatThrownBy(() -> service.accessUrl(14))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void platformAdminCannotReadChatImageWithoutMembership() {
        authenticate(1, 1, UserRole.PLATFORM_ADMIN);
        FileObject image = file(15, 100, 1, FileBusinessType.CHAT_IMAGE, 700L, FileStatus.APPROVED);
        when(fileObjects.selectOne(any())).thenReturn(image);
        when(access.countActiveConversationMember(700, 15, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.accessUrl(15))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void authenticate(long userId, long campusId, UserRole role) {
        authenticate(userId, campusId, role, VerificationStatus.APPROVED);
    }

    private void authenticate(long userId, long campusId, UserRole role, VerificationStatus verificationStatus) {
        AuthPrincipal principal = new AuthPrincipal(userId, campusId, role, UserStatus.ACTIVE,
                verificationStatus, 0, "test-token", Long.MAX_VALUE);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    private FileObject file(long id, long ownerId, long campusId, FileBusinessType type,
                            Long businessId, FileStatus status) {
        FileObject file = new FileObject();
        file.setId(id);
        file.setOwnerId(ownerId);
        file.setCampusId(campusId);
        file.setBusinessType(type);
        file.setBusinessId(businessId);
        file.setBucketName("test-bucket");
        file.setObjectKey("test/" + id + ".jpg");
        file.setContentType("image/jpeg");
        file.setByteSize(100L);
        file.setWidth(10);
        file.setHeight(10);
        file.setSortOrder(0);
        file.setStatus(status);
        file.setCreatedAt(Instant.parse("2026-07-12T00:00:00Z"));
        return file;
    }

    private SysUser user(long id, long campusId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setCampusId(campusId);
        user.setNickname("测试同学");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        return user;
    }
}
