package com.campusbuddies.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.activity.ActivityLifecycleStatus;
import com.campusbuddies.activity.ActivityModerationStatus;
import com.campusbuddies.activity.ActivityReviewStatus;
import com.campusbuddies.activity.BuddyActivity;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.activity.BuddyMemberMapper;
import com.campusbuddies.activity.ConversationMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.common.RequestIdFilter;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class GovernanceService {
    public record ReportView(long id, long campusId, ReportTargetType targetType, long targetId,
                             String reasonCode, String description, ReportStatus status,
                             String resolution, String appealReason, String appealResolution,
                             int version, Instant createdAt, Instant updatedAt) {}
    public record ReviewView(ReportView report, long reporterId, Long assigneeId) {}
    public record BlockedUserView(long id, String nickname) {}
    public record AuditView(long id, Long operatorId, String operatorRole, Long campusId,
                            String actionName, String targetType, String targetId,
                            String beforeState, String afterState, String reason,
                            String requestId, String ipAddress, Instant createdAt) {}

    private final ReportCaseMapper reports;
    private final ModerationActionMapper actions;
    private final AuditLogMapper audits;
    private final UserBlockMapper blocks;
    private final BuddyActivityMapper activities;
    private final BuddyMemberMapper members;
    private final ConversationMapper conversations;
    private final SysUserMapper users;
    private final ObjectMapper json;

    public GovernanceService(ReportCaseMapper reports, ModerationActionMapper actions, AuditLogMapper audits,
                             UserBlockMapper blocks, BuddyActivityMapper activities, BuddyMemberMapper members,
                             ConversationMapper conversations, SysUserMapper users, ObjectMapper json) {
        this.reports = reports;
        this.actions = actions;
        this.audits = audits;
        this.blocks = blocks;
        this.activities = activities;
        this.members = members;
        this.conversations = conversations;
        this.users = users;
        this.json = json;
    }

    @Transactional
    public ReportView submit(ReportTargetType type, long targetId, String reasonCode, String description) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        if (type == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "举报目标类型不能为空");
        long campusId = validateTargetForReporter(principal, type, targetId);
        String reason = normalizeReasonCode(reasonCode);
        String detail = optional(description, "举报说明", 1000);
        ReportCase report = new ReportCase();
        report.setCampusId(campusId);
        report.setReporterId(principal.userId());
        report.setTargetType(type);
        report.setTargetId(targetId);
        report.setReasonCode(reason);
        report.setDescription(detail);
        report.setStatus(ReportStatus.SUBMITTED);
        report.setVersion(0);
        reports.insert(report);
        audit(principal, "SUBMIT_REPORT", "REPORT", report.getId(), null,
                Map.of("status", ReportStatus.SUBMITTED.name()), detail);
        return view(report);
    }

    public PageResult<ReportView> mine(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        return pageReports(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getReporterId, principal.userId())
                .orderByDesc(ReportCase::getCreatedAt), page, size);
    }

    public PageResult<ReportView> affected(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        List<ReportCase> all = reports.selectList(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getCampusId, principal.campusId())
                .in(ReportCase::getStatus, ReportStatus.ACTIONED, ReportStatus.APPEALED,
                        ReportStatus.UPHELD, ReportStatus.REVOKED)
                .orderByDesc(ReportCase::getCreatedAt));
        List<ReportView> visible = all.stream().filter(report -> isAffected(report, principal.userId())).map(this::view).toList();
        int from = Math.min((page - 1) * safeSize, visible.size());
        int to = Math.min(from + safeSize, visible.size());
        return new PageResult<>(visible.subList(from, to), visible.size(), page, safeSize);
    }

    public PageResult<ReviewView> reviewQueue(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<ReportCase> query = new LambdaQueryWrapper<ReportCase>()
                .in(ReportCase::getStatus, principal.isPlatformAdmin()
                        ? List.of(ReportStatus.SUBMITTED, ReportStatus.REVIEWING, ReportStatus.APPEALED)
                        : List.of(ReportStatus.SUBMITTED, ReportStatus.REVIEWING))
                .orderByAsc(ReportCase::getCreatedAt);
        if (!principal.isPlatformAdmin()) query.eq(ReportCase::getCampusId, principal.campusId());
        IPage<ReportCase> result = reports.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream()
                .map(report -> new ReviewView(view(report), report.getReporterId(), report.getAssigneeId())).toList(),
                result.getTotal(), page, safeSize);
    }

    @Transactional
    public ReviewView claim(long id, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        ReportCase report = requireReport(id);
        SecuritySupport.requireCampus(principal, report.getCampusId());
        if (reports.claimWithExpiry(id, expectedVersion, principal.userId(), Instant.now().plus(15, ChronoUnit.MINUTES)) != 1) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_CLAIMED);
        }
        report = requireReport(id);
        audit(principal, "CLAIM_REPORT", "REPORT", id, null,
                Map.of("status", report.getStatus().name(), "assigneeId", principal.userId()), null);
        return new ReviewView(view(report), report.getReporterId(), report.getAssigneeId());
    }

    @Transactional
    public ReportView createCompletionDisputeCase(long reporterId, long activityId, String description) {
        BuddyActivity activity = activities.selectById(activityId);
        if (activity == null || activity.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!Objects.equals(activity.getCreatorId(), reporterId) && members.isActiveMember(activityId, reporterId) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        ReportCase existing = reports.selectOne(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getCampusId, activity.getCampusId())
                .eq(ReportCase::getTargetType, ReportTargetType.ACTIVITY)
                .eq(ReportCase::getTargetId, activityId)
                .eq(ReportCase::getReasonCode, "COMPLETION_DISPUTE")
                .in(ReportCase::getStatus, List.of(ReportStatus.SUBMITTED, ReportStatus.REVIEWING, ReportStatus.APPEALED)));
        if (existing != null) return view(existing);

        ReportCase report = new ReportCase();
        report.setCampusId(activity.getCampusId());
        report.setReporterId(reporterId);
        report.setTargetType(ReportTargetType.ACTIVITY);
        report.setTargetId(activityId);
        report.setReasonCode("COMPLETION_DISPUTE");
        report.setDescription(optional(description, "完成争议说明", 1000));
        report.setStatus(ReportStatus.SUBMITTED);
        report.setVersion(0);
        reports.insert(report);
        audit(SecuritySupport.current(), "AUTO_CREATE_COMPLETION_DISPUTE", "REPORT", report.getId(), null,
                Map.of("status", ReportStatus.SUBMITTED.name()), report.getDescription());
        return view(report);
    }

    @Transactional
    public ReviewView decide(long id, int expectedVersion, boolean actioned,
                             ModerationActionType actionType, String resolution, Integer durationHours) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        ReportCase report = requireReport(id);
        SecuritySupport.requireCampus(principal, report.getCampusId());
        String cleanResolution = required(resolution, "处置结论", 2, 1000);
        if (actioned) validateAction(report, actionType);
        ReportStatus target = actioned ? ReportStatus.ACTIONED : ReportStatus.DISMISSED;
        if (reports.decide(id, expectedVersion, principal.userId(), target, cleanResolution) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "案件状态或认领已变化，请刷新后重试");
        }
        if (actioned) applyAction(report, principal, actionType, cleanResolution, durationHours);
        audit(principal, actioned ? "ACTION_REPORT" : "DISMISS_REPORT", "REPORT", id,
                Map.of("status", ReportStatus.REVIEWING.name()), Map.of("status", target.name()), cleanResolution);
        report = requireReport(id);
        return new ReviewView(view(report), report.getReporterId(), report.getAssigneeId());
    }

    @Transactional
    public ReportView appeal(long id, int expectedVersion, String reason) {
        AuthPrincipal principal = SecuritySupport.current();
        ReportCase report = requireReport(id);
        SecuritySupport.requireCampus(principal, report.getCampusId());
        if (!isAffected(report, principal.userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        String cleanReason = required(reason, "申诉理由", 2, 1000);
        if (reports.appeal(id, expectedVersion, cleanReason) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前案件不能申诉或版本已变化");
        }
        audit(principal, "APPEAL_REPORT", "REPORT", id, Map.of("status", ReportStatus.ACTIONED.name()),
                Map.of("status", ReportStatus.APPEALED.name()), cleanReason);
        return view(requireReport(id));
    }

    @Transactional
    public ReviewView resolveAppeal(long id, int expectedVersion, boolean uphold, String resolution) {
        AuthPrincipal principal = SecuritySupport.current();
        if (!principal.isPlatformAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN, "仅平台管理员可复核申诉");
        ReportCase report = requireReport(id);
        String cleanResolution = required(resolution, "申诉复核结论", 2, 1000);
        ReportStatus target = uphold ? ReportStatus.UPHELD : ReportStatus.REVOKED;
        if (reports.resolveAppeal(id, expectedVersion, target, cleanResolution) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "申诉状态已变化，请刷新后重试");
        }
        if (!uphold) reverseAction(report, principal, cleanResolution);
        audit(principal, uphold ? "UPHOLD_APPEAL" : "REVOKE_ACTION", "REPORT", id,
                Map.of("status", ReportStatus.APPEALED.name()), Map.of("status", target.name()), cleanResolution);
        report = requireReport(id);
        return new ReviewView(view(report), report.getReporterId(), report.getAssigneeId());
    }

    @Transactional
    public void block(long blockedId) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        if (principal.userId() == blockedId) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能拉黑自己");
        SysUser blocked = users.selectById(blockedId);
        if (blocked == null || blocked.getRole() != UserRole.STUDENT
                || !Objects.equals(blocked.getCampusId(), principal.campusId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        blocks.insertIgnore(principal.userId(), blockedId);
    }

    @Transactional
    public void unblock(long blockedId) {
        blocks.delete(SecuritySupport.current().userId(), blockedId);
    }

    public List<BlockedUserView> blockedUsers() {
        return blocks.findBlocked(SecuritySupport.current().userId()).stream()
                .map(user -> new BlockedUserView(user.getId(), user.getNickname())).toList();
    }

    public PageResult<AuditView> auditLogs(int page, int size, String actionName) {
        AuthPrincipal principal = SecuritySupport.current();
        if (!principal.isPlatformAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<AuditLog> query = new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreatedAt);
        if (StringUtils.hasText(actionName)) query.eq(AuditLog::getActionName, actionName.trim());
        IPage<AuditLog> result = audits.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(item -> new AuditView(
                        item.getId(), item.getOperatorId(), item.getOperatorRole(), item.getCampusId(),
                        item.getActionName(), item.getTargetType(), item.getTargetId(), item.getBeforeState(),
                        item.getAfterState(), item.getReason(), item.getRequestId(), item.getIpAddress(), item.getCreatedAt()))
                .toList(), result.getTotal(), page, safeSize);
    }

    private long validateTargetForReporter(AuthPrincipal principal, ReportTargetType type, long targetId) {
        if (type == ReportTargetType.USER) {
            if (targetId == principal.userId()) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能举报自己");
            SysUser target = users.selectById(targetId);
            if (target == null || target.getRole() != UserRole.STUDENT
                    || !Objects.equals(target.getCampusId(), principal.campusId())) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            return target.getCampusId();
        }
        BuddyActivity activity = activities.selectById(targetId);
        if (activity == null || activity.getDeletedAt() != null
                || !Objects.equals(activity.getCampusId(), principal.campusId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (activity.getCreatorId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能举报自己发布的活动");
        }
        boolean visible = activity.getReviewStatus() == ActivityReviewStatus.APPROVED
                && activity.getModerationStatus() == ActivityModerationStatus.NORMAL;
        if (!visible && members.isActiveMember(targetId, principal.userId()) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return activity.getCampusId();
    }

    private void validateAction(ReportCase report, ModerationActionType actionType) {
        if (actionType == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择处置动作");
        boolean valid = report.getTargetType() == ReportTargetType.ACTIVITY
                ? actionType == ModerationActionType.REMOVE_ACTIVITY
                : actionType == ModerationActionType.LIMIT_USER || actionType == ModerationActionType.SUSPEND_USER;
        if (!valid) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "处置动作与举报目标不匹配");
    }

    private void applyAction(ReportCase report, AuthPrincipal operator, ModerationActionType type,
                             String reason, Integer durationHours) {
        Instant expiresAt = null;
        if (report.getTargetType() == ReportTargetType.ACTIVITY) {
            if (activities.setModerationStatus(report.getTargetId(), ActivityModerationStatus.REMOVED) != 1) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
            }
            conversations.updateStatus(report.getTargetId(), "LOCKED");
        } else {
            UserStatus status = type == ModerationActionType.LIMIT_USER ? UserStatus.LIMITED : UserStatus.SUSPENDED;
            if (users.setStatusAndInvalidateTokens(report.getTargetId(), status) != 1) {
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "只能处置学生账号");
            }
            if (durationHours != null) {
                if (durationHours < 1 || durationHours > 24 * 30) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "限制时长必须在 1 小时到 30 天之间");
                }
                expiresAt = Instant.now().plus(durationHours, ChronoUnit.HOURS);
            }
        }
        insertAction(report, operator.userId(), type, reason, expiresAt);
    }

    private void reverseAction(ReportCase report, AuthPrincipal operator, String reason) {
        ModerationAction original = actions.findLatest(report.getId());
        if (original == null) throw new BusinessException(ErrorCode.CONFLICT, "案件缺少原处置记录");
        if (original.getActionType() == ModerationActionType.REMOVE_ACTIVITY) {
            activities.setModerationStatus(report.getTargetId(), ActivityModerationStatus.NORMAL);
            BuddyActivity activity = activities.selectById(report.getTargetId());
            String conversationStatus = activity != null
                    && (activity.getLifecycleStatus() == ActivityLifecycleStatus.RECRUITING
                        || activity.getLifecycleStatus() == ActivityLifecycleStatus.IN_PROGRESS) ? "OPEN" : "READ_ONLY";
            conversations.updateStatus(report.getTargetId(), conversationStatus);
            insertAction(report, operator.userId(), ModerationActionType.RESTORE_ACTIVITY, reason, null);
        } else if (original.getActionType() == ModerationActionType.LIMIT_USER
                || original.getActionType() == ModerationActionType.SUSPEND_USER) {
            users.setStatusAndInvalidateTokens(report.getTargetId(), UserStatus.ACTIVE);
            insertAction(report, operator.userId(), ModerationActionType.RESTORE_USER, reason, null);
        } else {
            throw new BusinessException(ErrorCode.CONFLICT, "原处置动作不可撤销");
        }
    }

    private void insertAction(ReportCase report, long operatorId, ModerationActionType type,
                              String reason, Instant expiresAt) {
        ModerationAction action = new ModerationAction();
        action.setReportId(report.getId());
        action.setOperatorId(operatorId);
        action.setTargetType(report.getTargetType());
        action.setTargetId(report.getTargetId());
        action.setActionType(type);
        action.setReason(reason);
        action.setExpiresAt(expiresAt);
        actions.insert(action);
    }

    private boolean isAffected(ReportCase report, long userId) {
        if (report.getTargetType() == ReportTargetType.USER) return report.getTargetId() == userId;
        BuddyActivity activity = activities.selectById(report.getTargetId());
        return activity != null && activity.getCreatorId() == userId;
    }

    private PageResult<ReportView> pageReports(LambdaQueryWrapper<ReportCase> query, int page, int size) {
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        IPage<ReportCase> result = reports.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(), result.getTotal(), page, safeSize);
    }

    private ReportCase requireReport(long id) {
        ReportCase report = reports.selectById(id);
        if (report == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return report;
    }

    private ReportView view(ReportCase report) {
        return new ReportView(report.getId(), report.getCampusId(), report.getTargetType(), report.getTargetId(),
                report.getReasonCode(), report.getDescription(), report.getStatus(), report.getResolution(),
                report.getAppealReason(), report.getAppealResolution(),
                report.getVersion() == null ? 0 : report.getVersion(), report.getCreatedAt(), report.getUpdatedAt());
    }

    private void audit(AuthPrincipal operator, String actionName, String targetType, long targetId,
                       Object before, Object after, String reason) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operator.userId());
        log.setOperatorRole(operator.role().name());
        log.setCampusId(operator.campusId());
        log.setActionName(actionName);
        log.setTargetType(targetType);
        log.setTargetId(String.valueOf(targetId));
        log.setBeforeState(toJson(before));
        log.setAfterState(toJson(after));
        log.setReason(reason == null || reason.length() <= 500 ? reason : reason.substring(0, 500));
        log.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) log.setIpAddress(attributes.getRequest().getRemoteAddr());
        audits.insert(log);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "审计数据序列化失败");
        }
    }

    private static String normalizeReasonCode(String value) {
        if (!StringUtils.hasText(value)) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "举报原因不能为空");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9_]{2,40}")) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "举报原因代码格式不正确");
        }
        return normalized;
    }

    private static String required(String value, String label, int min, int max) {
        String cleaned = optional(value, label, max);
        if (cleaned == null || cleaned.codePointCount(0, cleaned.length()) < min) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "至少 " + min + " 字");
        }
        return cleaned;
    }

    private static String optional(String value, String label, int max) {
        if (!StringUtils.hasText(value)) return null;
        String cleaned = value.trim();
        if (cleaned.codePointCount(0, cleaned.length()) > max) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "不能超过 " + max + " 字");
        }
        return cleaned;
    }
}
