package com.campusbuddies.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.activity.ActivityLifecycleStatus;
import com.campusbuddies.activity.ActivityModerationStatus;
import com.campusbuddies.activity.ActivityReviewStatus;
import com.campusbuddies.activity.BuddyActivity;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.campus.Campus;
import com.campusbuddies.campus.CampusMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.config.AppProperties;
import com.campusbuddies.notification.NotificationService;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    public record FileView(long id, long ownerId, long campusId, FileBusinessType businessType,
                           Long businessId, String originalName, String contentType, long byteSize,
                           int width, int height, FileStatus status, String scanResult, int sortOrder,
                           Instant createdAt) {}
    public record FileUrl(long id, String url, long expiresInSeconds) {}

    private final FileObjectMapper files;
    private final ActivityMediaMapper activityMedia;
    private final BuddyActivityMapper activities;
    private final CampusMapper campuses;
    private final ObjectStorage storage;
    private final ImageSanitizer sanitizer;
    private final NotificationService notifications;
    private final FileAccessMapper access;
    private final String bucket;

    public FileService(FileObjectMapper files, ActivityMediaMapper activityMedia,
                       BuddyActivityMapper activities, CampusMapper campuses,
                       ObjectStorage storage, ImageSanitizer sanitizer, AppProperties.Minio properties,
                       NotificationService notifications, FileAccessMapper access) {
        this.files = files;
        this.activityMedia = activityMedia;
        this.activities = activities;
        this.campuses = campuses;
        this.storage = storage;
        this.sanitizer = sanitizer;
        this.notifications = notifications;
        this.access = access;
        this.bucket = properties.bucket();
    }

    @Transactional
    public FileView upload(MultipartFile upload, FileBusinessType businessType, Long requestedCampusId) {
        AuthPrincipal principal = SecuritySupport.current();
        if (principal.role() != UserRole.STUDENT || principal.status() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (businessType == FileBusinessType.AVATAR) SecuritySupport.requireVerifiedStudent(principal);
        if (upload == null || upload.isEmpty() || businessType == null) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "请选择图片和用途");
        }
        long campusId = resolveCampus(principal, businessType, requestedCampusId);
        byte[] source = readBounded(upload, businessType.maxBytes());
        ImageSanitizer.Sanitized safe = sanitizer.sanitize(source);
        if (safe.bytes().length > businessType.maxBytes()) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "安全重编码后的图片超过大小限制");
        }
        if (!StringUtils.hasText(bucket)) throw new StorageUnavailableException();
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String objectKey = "pending/%d/%02d/%s.%s".formatted(date.getYear(), date.getMonthValue(),
                UUID.randomUUID(), safe.extension());
        storage.put(bucket, objectKey, new ByteArrayInputStream(safe.bytes()), safe.bytes().length, safe.contentType());

        FileObject file = new FileObject();
        file.setOwnerId(principal.userId());
        file.setCampusId(campusId);
        file.setBusinessType(businessType);
        file.setBucketName(bucket);
        file.setObjectKey(objectKey);
        file.setOriginalName(safeOriginalName(upload.getOriginalFilename()));
        file.setContentType(safe.contentType());
        file.setByteSize((long) safe.bytes().length);
        file.setWidth(safe.width());
        file.setHeight(safe.height());
        file.setSha256(safe.sha256());
        file.setStatus(FileStatus.PENDING_SCAN);
        file.setSortOrder(0);
        try {
            files.insert(file);
        } catch (RuntimeException ex) {
            try { storage.delete(bucket, objectKey); } catch (RuntimeException ignored) { }
            throw ex;
        }
        return view(file);
    }

    public PageResult<FileView> mine(int page, int size) {
        long ownerId = SecuritySupport.current().userId();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<FileObject> query = new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getOwnerId, ownerId)
                .isNull(FileObject::getDeletedAt)
                .orderByDesc(FileObject::getCreatedAt)
                .orderByDesc(FileObject::getId);
        IPage<FileObject> result = files.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getTotal(), page, safeSize);
    }

    public FileView mine(long id) {
        long ownerId = SecuritySupport.current().userId();
        FileObject file = files.selectOne(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getId, id)
                .eq(FileObject::getOwnerId, ownerId)
                .isNull(FileObject::getDeletedAt));
        if (file == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return view(file);
    }

    public PageResult<FileView> reviewQueue(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<FileObject> query = new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getStatus, FileStatus.PENDING_SCAN)
                .isNull(FileObject::getDeletedAt)
                .orderByAsc(FileObject::getCreatedAt);
        if (!principal.isPlatformAdmin()) query.eq(FileObject::getCampusId, principal.campusId());
        IPage<FileObject> result = files.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getTotal(), page, safeSize);
    }

    @Transactional
    public FileView moderate(long id, boolean approve, String reason) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        FileObject file = requireFile(id);
        SecuritySupport.requireCampus(principal, file.getCampusId());
        String result;
        if (approve) {
            result = "MANUAL_APPROVED";
        } else {
            if (!StringUtils.hasText(reason)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "驳回时必须填写原因");
            result = "REJECTED:" + reason.trim();
            if (result.length() > 80) result = result.substring(0, 80);
        }
        FileStatus target = approve ? FileStatus.APPROVED : FileStatus.REJECTED;
        if (files.moderate(id, target, result) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "文件状态已变化，请刷新后重试");
        }
        notifications.create(file.getOwnerId(), approve ? "FILE_APPROVED" : "FILE_REJECTED",
                approve ? "图片审核已通过" : "图片审核未通过",
                approve ? "您上传的图片已通过审核，可以用于对应功能。" : "审核原因：" + reason.trim(),
                "FILE", id);
        return view(requireFile(id));
    }

    public FileUrl accessUrl(long id) {
        AuthPrincipal principal = SecuritySupport.current();
        FileObject file = requireFile(id);
        boolean authorized;
        if (file.getBusinessType() == FileBusinessType.CHAT_IMAGE) {
            // Chat media is private to active conversation members, including for reviewers.
            if (principal.isReviewer()) SecuritySupport.requireCampus(principal, file.getCampusId());
            authorized = file.getStatus() == FileStatus.APPROVED
                    && file.getBusinessId() != null
                    && access.countActiveConversationMember(file.getBusinessId(), id, principal.userId()) > 0;
        } else {
            authorized = file.getOwnerId().equals(principal.userId());
            if (principal.isReviewer()) {
                SecuritySupport.requireCampus(principal, file.getCampusId());
                authorized = true;
            }
            if (!authorized && file.getStatus() == FileStatus.APPROVED
                    && Objects.equals(file.getCampusId(), principal.campusId())) {
                if (file.getBusinessType() == FileBusinessType.AVATAR
                        && file.getBusinessId() != null
                        && Objects.equals(file.getBusinessId(), file.getOwnerId())
                        && access.countActiveAvatar(id, file.getOwnerId()) > 0) authorized = true;
                if (file.getBusinessType() == FileBusinessType.ACTIVITY_IMAGE && file.getBusinessId() != null) {
                    BuddyActivity activity = activities.selectById(file.getBusinessId());
                    authorized = activity != null && activity.getReviewStatus() == ActivityReviewStatus.APPROVED
                            && activity.getModerationStatus() == ActivityModerationStatus.NORMAL;
                }
            }
        }
        if (!authorized) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        Duration ttl = Duration.ofMinutes(10);
        return new FileUrl(id, storage.presignedGetUrl(file.getBucketName(), file.getObjectKey(), ttl), ttl.toSeconds());
    }

    @Transactional
    public void delete(long id) {
        AuthPrincipal principal = SecuritySupport.current();
        FileObject file = requireFile(id);
        if (files.softDeleteUnbound(id, principal.userId()) != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能删除本人尚未绑定业务的文件");
        }
        storage.delete(file.getBucketName(), file.getObjectKey());
    }

    public void requireOwnedEvidence(long fileId, long ownerId, long campusId, FileBusinessType type) {
        FileObject file = files.selectById(fileId);
        if (file == null || !file.getOwnerId().equals(ownerId) || !file.getCampusId().equals(campusId)
                || file.getBusinessType() != type || file.getDeletedAt() != null
                || file.getStatus() == FileStatus.REJECTED || file.getStatus() == FileStatus.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "证明文件无效或不属于当前用户");
        }
    }

    public void requireApprovedEvidence(long fileId, long ownerId, long campusId,
                                        FileBusinessType type, long businessId) {
        FileObject file = files.selectById(fileId);
        if (file == null || !file.getOwnerId().equals(ownerId) || !file.getCampusId().equals(campusId)
                || file.getBusinessType() != type || file.getStatus() != FileStatus.APPROVED
                || !Objects.equals(file.getBusinessId(), businessId) || file.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "证明文件尚未审核通过或绑定关系无效");
        }
    }

    @Transactional
    public void replaceAvatar(long fileId, Long currentFileId, long ownerId, Long campusId) {
        if (campusId == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "完成校园认证后才能设置头像");
        FileObject file = requireFile(fileId);
        if (!validAvatar(file, ownerId, campusId,
                Objects.equals(currentFileId, fileId) ? ownerId : null)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "头像无效、未审核或已被使用");
        }
        if (Objects.equals(currentFileId, fileId)) return;
        if (files.bind(fileId, ownerId, FileBusinessType.AVATAR, ownerId, 0) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "头像状态已变化，请重新选择");
        }
        if (currentFileId != null) files.unbindAvatar(currentFileId, ownerId);
    }

    public void requireOwnedApprovedChatImage(long fileId, long ownerId, long campusId) {
        FileObject file = files.selectById(fileId);
        if (file == null || !file.getOwnerId().equals(ownerId) || !file.getCampusId().equals(campusId)
                || file.getBusinessType() != FileBusinessType.CHAT_IMAGE
                || file.getStatus() != FileStatus.APPROVED || file.getBusinessId() != null
                || file.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "聊天图片无效、未审核或已被使用");
        }
    }

    @Transactional
    public void bindChatImage(long fileId, long ownerId, long messageId) {
        if (files.bind(fileId, ownerId, FileBusinessType.CHAT_IMAGE, messageId, 0) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "聊天图片状态已变化，请重新选择");
        }
    }

    @Transactional
    public void bindEvidence(long fileId, long ownerId, long campusId,
                             FileBusinessType type, long businessId) {
        if (files.bindEvidence(fileId, ownerId, campusId, type, businessId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "证明文件已绑定或状态已变化");
        }
    }

    @Transactional
    public List<Long> replaceActivityMedia(long activityId, List<Long> fileIds) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity activity = activities.selectById(activityId);
        if (activity == null || !activity.getCreatorId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.DRAFT
                || (activity.getReviewStatus() != ActivityReviewStatus.NOT_SUBMITTED
                    && activity.getReviewStatus() != ActivityReviewStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前活动不能修改图片");
        }
        List<Long> unique = fileIds == null ? List.of() : fileIds.stream().distinct().toList();
        if (unique.size() > 6) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "活动图片最多 6 张");
        for (Long fileId : unique) {
            FileObject file = requireFile(fileId);
            if (!file.getOwnerId().equals(principal.userId()) || !file.getCampusId().equals(activity.getCampusId())
                    || file.getBusinessType() != FileBusinessType.ACTIVITY_IMAGE
                    || file.getStatus() != FileStatus.APPROVED
                    || (file.getBusinessId() != null && !file.getBusinessId().equals(activityId))) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "活动图片无效、未审核或不属于当前用户");
            }
        }
        activityMedia.deleteByActivity(activityId);
        files.unbindBusiness(principal.userId(), FileBusinessType.ACTIVITY_IMAGE, activityId);
        for (int index = 0; index < unique.size(); index++) {
            long fileId = unique.get(index);
            if (files.bind(fileId, principal.userId(), FileBusinessType.ACTIVITY_IMAGE, activityId, index) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "活动图片状态已变化，请刷新后重试");
            }
            activityMedia.insert(activityId, fileId, index);
        }
        return unique;
    }

    private long resolveCampus(AuthPrincipal principal, FileBusinessType type, Long requestedCampusId) {
        Long campusId = principal.campusId();
        if (campusId == null) {
            if (type != FileBusinessType.IDENTITY_PROOF || requestedCampusId == null) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "认证证明上传前必须选择校园");
            }
            campusId = requestedCampusId;
        } else if (requestedCampusId != null && !campusId.equals(requestedCampusId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止跨校园上传");
        }
        Campus campus = campuses.selectById(campusId);
        if (campus == null || !"ACTIVE".equals(campus.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "校园不存在或已停用");
        }
        return campusId;
    }

    private byte[] readBounded(MultipartFile upload, long maxBytes) {
        if (upload.getSize() <= 0 || upload.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "图片大小超过该用途限制");
        }
        try (InputStream input = upload.getInputStream()) {
            byte[] value = input.readNBytes(Math.toIntExact(maxBytes + 1));
            if (value.length > maxBytes || input.read() != -1) {
                throw new BusinessException(ErrorCode.FILE_REJECTED, "图片大小超过该用途限制");
            }
            return value;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.FILE_REJECTED, "读取上传图片失败");
        }
    }

    private String safeOriginalName(String value) {
        if (!StringUtils.hasText(value)) return null;
        String safe = value.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private FileObject requireFile(long id) {
        FileObject file = files.selectOne(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getId, id).isNull(FileObject::getDeletedAt));
        if (file == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return file;
    }

    private boolean validAvatar(FileObject file, long ownerId, long campusId, Long businessId) {
        return file.getOwnerId().equals(ownerId) && file.getCampusId().equals(campusId)
                && file.getBusinessType() == FileBusinessType.AVATAR
                && file.getStatus() == FileStatus.APPROVED
                && Objects.equals(file.getBusinessId(), businessId) && file.getDeletedAt() == null;
    }

    private FileView view(FileObject file) {
        return new FileView(file.getId(), file.getOwnerId(), file.getCampusId(), file.getBusinessType(),
                file.getBusinessId(), file.getOriginalName(), file.getContentType(), file.getByteSize(),
                file.getWidth(), file.getHeight(), file.getStatus(), file.getScanResult(),
                file.getSortOrder() == null ? 0 : file.getSortOrder(), file.getCreatedAt());
    }
}
