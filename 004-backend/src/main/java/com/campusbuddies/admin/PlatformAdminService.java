package com.campusbuddies.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.campus.Campus;
import com.campusbuddies.campus.CampusMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.common.RequestIdFilter;
import com.campusbuddies.governance.AuditLog;
import com.campusbuddies.governance.AuditLogMapper;
import com.campusbuddies.activity.ActivityTag;
import com.campusbuddies.activity.ActivityTagMapper;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformAdminService {
    public record UserView(long id, Long campusId, String campusName, String username, String wechatOpenid,
                           String nickname, Long avatarFileId, String bio, String gradeName, String majorName,
                           String interestTagsJson, UserRole role, UserStatus status,
                           VerificationStatus verificationStatus, int tokenVersion,
                           Instant createdAt, Instant updatedAt) {}

    public record UserCommand(String nickname, Long campusId, UserRole role, UserStatus status) {}

    public record CampusView(long id, String name, String code, String status, String identityLabel,
                             Instant createdAt, Instant updatedAt) {}

    public record CampusCommand(String name, String code, String status, String identityLabel) {}

    public record TagView(long id, long campusId, String campusName, String name, String normalizedName,
                          String status, Instant createdAt) {}

    public record TagCreateCommand(Long campusId, String name, String status) {}

    public record TagUpdateCommand(String name, String status) {}

    private final SysUserMapper users;
    private final CampusMapper campuses;
    private final ActivityTagMapper tags;
    private final AuditLogMapper audits;
    private final ObjectMapper json;

    public PlatformAdminService(SysUserMapper users, CampusMapper campuses, ActivityTagMapper tags,
                                AuditLogMapper audits, ObjectMapper json) {
        this.users = users;
        this.campuses = campuses;
        this.tags = tags;
        this.audits = audits;
        this.json = json;
    }

    public PageResult<UserView> users(int page, int size, String keyword, UserRole role, UserStatus status, Long campusId) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        String queryText = clean(keyword);
        if (queryText != null && codePoints(queryText) > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "搜索词不能超过 50 字");
        }
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>()
                .apply("deleted_at IS NULL")
                .orderByDesc(SysUser::getCreatedAt);
        if (campusId != null) query.eq(SysUser::getCampusId, campusId);
        if (role != null) query.eq(SysUser::getRole, role);
        if (status != null) query.eq(SysUser::getStatus, status);
        if (queryText != null) {
            query.and(group -> group.like(SysUser::getNickname, queryText)
                    .or().like(SysUser::getUsername, queryText)
                    .or().like(SysUser::getWechatOpenid, queryText));
        }
        IPage<SysUser> result = users.selectPage(Page.of(page, safeSize), query);
        Map<Long, String> campusNames = campusNames();
        List<UserView> records = result.getRecords().stream().map(user -> view(user, campusNames)).toList();
        return new PageResult<>(records, result.getTotal(), page, safeSize);
    }

    @Transactional
    public UserView updateUser(long id, UserCommand input) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requirePlatformAdmin(principal);
        SysUser user = users.findByIdForUpdate(id);
        if (user == null || user.getStatus() == UserStatus.CLOSED) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (user.getRole() == UserRole.PLATFORM_ADMIN || input.role() == UserRole.PLATFORM_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "平台管理员账号不在此处配置");
        }
        if (principal.userId() == id && (input.role() != null || input.status() != null || input.campusId() != null)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能修改自己的权限和状态");
        }
        String before = snapshot(user);
        boolean changed = false;
        String nickname = required(input.nickname(), "昵称", 1, 40);
        if (!Objects.equals(user.getNickname(), nickname)) {
            user.setNickname(nickname);
            changed = true;
        }
        UserRole role = input.role() == null ? user.getRole() : input.role();
        if (role == UserRole.PLATFORM_ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "平台管理员账号不在此处配置");
        }
        Long campusId = input.campusId() != null ? input.campusId() : user.getCampusId();
        if (role == UserRole.CAMPUS_REVIEWER) {
            campusId = requireActiveCampus(campusId);
        } else if (campusId != null) {
            campusId = requireActiveCampus(campusId);
        }
        if (!Objects.equals(user.getRole(), role)) {
            user.setRole(role);
            changed = true;
        }
        if (!Objects.equals(user.getCampusId(), campusId)) {
            user.setCampusId(campusId);
            changed = true;
        }
        UserStatus status = input.status() == null ? user.getStatus() : input.status();
        if (!Objects.equals(user.getStatus(), status)) {
            user.setStatus(status);
            changed = true;
        }
        if (changed) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
            users.updateById(user);
            audit("ADMIN_USER_UPDATE", "USER", String.valueOf(user.getId()), before, snapshot(user),
                    "更新用户权限与基础信息");
        }
        return view(user, campusNames());
    }

    public PageResult<CampusView> campuses(int page, int size, String keyword, String status) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        String queryText = clean(keyword);
        if (queryText != null && codePoints(queryText) > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "搜索词不能超过 50 字");
        }
        LambdaQueryWrapper<Campus> query = new LambdaQueryWrapper<Campus>().orderByDesc(Campus::getId);
        if (StringUtils.hasText(status)) query.eq(Campus::getStatus, cleanStatus(status));
        if (queryText != null) {
            query.and(group -> group.like(Campus::getName, queryText)
                    .or().like(Campus::getCode, queryText)
                    .or().like(Campus::getIdentityLabel, queryText));
        }
        IPage<Campus> result = campuses.selectPage(Page.of(page, safeSize), query);
        List<CampusView> records = result.getRecords().stream().map(this::view).toList();
        return new PageResult<>(records, result.getTotal(), page, safeSize);
    }

    @Transactional
    public CampusView createCampus(CampusCommand input) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        Campus campus = new Campus();
        campus.id = IdWorker.getId();
        campus.name = required(input.name(), "校园名称", 2, 80);
        campus.code = required(input.code(), "校园代码", 2, 32);
        campus.status = cleanStatus(input.status(), "ACTIVE");
        campus.identityLabel = required(input.identityLabel(), "认证标识", 1, 40);
        try {
            campuses.insert(campus);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "校园代码已存在");
        }
        audit("ADMIN_CAMPUS_CREATE", "CAMPUS", String.valueOf(campus.id), null, snapshot(campus), "新增校园");
        return view(campus);
    }

    @Transactional
    public CampusView updateCampus(long id, CampusCommand input) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        Campus campus = campuses.selectById(id);
        if (campus == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        String before = snapshot(campus);
        campus.name = required(input.name(), "校园名称", 2, 80);
        campus.code = required(input.code(), "校园代码", 2, 32);
        campus.status = cleanStatus(input.status(), "ACTIVE");
        campus.identityLabel = required(input.identityLabel(), "认证标识", 1, 40);
        try {
            campuses.updateById(campus);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "校园代码已存在");
        }
        audit("ADMIN_CAMPUS_UPDATE", "CAMPUS", String.valueOf(campus.id), before, snapshot(campus), "更新校园");
        return view(campus);
    }

    public PageResult<TagView> tags(int page, int size, Long campusId, String keyword, String status) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        String queryText = clean(keyword);
        if (queryText != null && codePoints(queryText) > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "搜索词不能超过 50 字");
        }
        LambdaQueryWrapper<ActivityTag> query = new LambdaQueryWrapper<ActivityTag>().orderByDesc(ActivityTag::getCreatedAt);
        if (campusId != null) query.eq(ActivityTag::getCampusId, campusId);
        if (StringUtils.hasText(status)) query.eq(ActivityTag::getStatus, cleanStatus(status));
        if (queryText != null) query.like(ActivityTag::getName, queryText);
        IPage<ActivityTag> result = tags.selectPage(Page.of(page, safeSize), query);
        Map<Long, String> campusNames = campusNames();
        List<TagView> records = result.getRecords().stream().map(tag -> view(tag, campusNames)).toList();
        return new PageResult<>(records, result.getTotal(), page, safeSize);
    }

    @Transactional
    public TagView createTag(TagCreateCommand input) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        long campusId = requireActiveCampus(input.campusId());
        String name = required(input.name(), "标签名称", 2, 12);
        ActivityTag tag = new ActivityTag();
        tag.setId(IdWorker.getId());
        tag.setCampusId(campusId);
        tag.setName(name);
        tag.setNormalizedName(normalizedName(name));
        tag.setStatus(cleanStatus(input.status(), "ACTIVE"));
        try {
            tags.insert(tag);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "该校园下标签已存在");
        }
        audit("ADMIN_TAG_CREATE", "TAG", String.valueOf(tag.getId()), null, snapshot(tag), "新增推荐标签");
        return view(tag, campusNames());
    }

    @Transactional
    public TagView updateTag(long id, TagUpdateCommand input) {
        SecuritySupport.requirePlatformAdmin(SecuritySupport.current());
        ActivityTag tag = tags.selectById(id);
        if (tag == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        String before = snapshot(tag);
        tag.setName(required(input.name(), "标签名称", 2, 12));
        tag.setNormalizedName(normalizedName(tag.getName()));
        tag.setStatus(cleanStatus(input.status(), "ACTIVE"));
        try {
            tags.updateById(tag);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "该校园下标签已存在");
        }
        audit("ADMIN_TAG_UPDATE", "TAG", String.valueOf(tag.getId()), before, snapshot(tag), "更新推荐标签");
        return view(tag, campusNames());
    }

    private long requireActiveCampus(Long campusId) {
        if (campusId == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择校园");
        Campus campus = campuses.selectById(campusId);
        if (campus == null || !"ACTIVE".equals(campus.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "校园不存在或未启用");
        }
        return campusId;
    }

    private CampusView view(Campus campus) {
        return new CampusView(campus.getId(), campus.getName(), campus.getCode(), campus.getStatus(),
                campus.getIdentityLabel(), campus.getCreatedAt(), campus.getUpdatedAt());
    }

    private TagView view(ActivityTag tag, Map<Long, String> campusNames) {
        return new TagView(tag.getId(), tag.getCampusId(), campusNames.get(tag.getCampusId()),
                tag.getName(), tag.getNormalizedName(), tag.getStatus(), tag.getCreatedAt());
    }

    private UserView view(SysUser user, Map<Long, String> campusNames) {
        return new UserView(user.getId(), user.getCampusId(), campusNames.get(user.getCampusId()),
                user.getUsername(), user.getWechatOpenid(), user.getNickname(), user.getAvatarFileId(),
                user.getBio(), user.getGradeName(), user.getMajorName(), user.getInterestTagsJson(),
                user.getRole(), user.getStatus(), user.getVerificationStatus(),
                user.getTokenVersion() == null ? 0 : user.getTokenVersion(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private Map<Long, String> campusNames() {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Campus campus : campuses.selectList(new LambdaQueryWrapper<Campus>().orderByAsc(Campus::getId))) {
            result.put(campus.getId(), campus.getName());
        }
        return result;
    }

    private void audit(String action, String targetType, String targetId, String before, String after, String reason) {
        AuthPrincipal operator = SecuritySupport.current();
        AuditLog log = new AuditLog();
        log.setId(IdWorker.getId());
        log.setOperatorId(operator.userId());
        log.setOperatorRole(operator.role().name());
        log.setCampusId(operator.campusId());
        log.setActionName(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setBeforeState(before);
        log.setAfterState(after);
        log.setReason(reason);
        log.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        audits.insert(log);
    }

    private String snapshot(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private static String clean(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String required(String value, String label, int min, int max) {
        String cleaned = clean(value);
        int length = cleaned == null ? 0 : codePoints(cleaned);
        if (length < min || length > max) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "长度必须在 " + min + " 到 " + max + " 字之间");
        }
        return cleaned;
    }

    private static String cleanStatus(String status) {
        String cleaned = clean(status);
        if (!StringUtils.hasText(cleaned)) {
            return "ACTIVE";
        }
        return cleanStatus(cleaned, cleaned);
    }

    private static String cleanStatus(String status, String defaultValue) {
        String cleaned = clean(status);
        if (!StringUtils.hasText(cleaned)) {
            cleaned = defaultValue;
        }
        if (!"ACTIVE".equals(cleaned) && !"INACTIVE".equals(cleaned)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "状态值不合法");
        }
        return cleaned;
    }

    private static String normalizedName(String value) {
        return Normalizer.normalize(clean(value), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }
}
