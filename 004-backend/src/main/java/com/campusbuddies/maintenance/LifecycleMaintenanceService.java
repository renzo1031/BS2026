package com.campusbuddies.maintenance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbuddies.activity.ActivityLifecycleStatus;
import com.campusbuddies.activity.ActivityStatusLog;
import com.campusbuddies.activity.ActivityStatusLogMapper;
import com.campusbuddies.activity.BuddyActivity;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.activity.BuddyMember;
import com.campusbuddies.activity.BuddyMemberMapper;
import com.campusbuddies.activity.CompletionStatus;
import com.campusbuddies.activity.ConversationMapper;
import com.campusbuddies.common.RequestIdFilter;
import com.campusbuddies.governance.AuditLog;
import com.campusbuddies.governance.AuditLogMapper;
import com.campusbuddies.governance.ReportCase;
import com.campusbuddies.governance.ReportCaseMapper;
import com.campusbuddies.governance.ReportStatus;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LifecycleMaintenanceService {
    private static final long SYSTEM_OPERATOR_ID = 1L;

    private final BuddyActivityMapper activities;
    private final BuddyMemberMapper members;
    private final ConversationMapper conversations;
    private final ReportCaseMapper reports;
    private final AuditLogMapper audits;
    private final ActivityStatusLogMapper statusLogs;
    private final SysUserMapper users;
    private final ObjectMapper json;

    public LifecycleMaintenanceService(BuddyActivityMapper activities, BuddyMemberMapper members,
                                       ConversationMapper conversations, ReportCaseMapper reports,
                                       AuditLogMapper audits, ActivityStatusLogMapper statusLogs,
                                       SysUserMapper users, ObjectMapper json) {
        this.activities = activities;
        this.members = members;
        this.conversations = conversations;
        this.reports = reports;
        this.audits = audits;
        this.statusLogs = statusLogs;
        this.users = users;
        this.json = json;
    }

    @Scheduled(fixedDelayString = "${campus-buddy.maintenance.interval-ms:60000}")
    @Transactional
    public void maintain() {
        runOnce(Instant.now());
    }

    @Transactional
    public void runOnce(Instant now) {
        releaseExpiredActivityReviewClaims(now);
        expireRecruitingActivities(now);
        autoCompleteTimedOutActivities(now);
        releaseExpiredReportClaims(now);
    }

    private void releaseExpiredActivityReviewClaims(Instant now) {
        List<BuddyActivity> expired = activities.selectList(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getReviewStatus, com.campusbuddies.activity.ActivityReviewStatus.PENDING)
                .isNotNull(BuddyActivity::getClaimExpiresAt)
                .lt(BuddyActivity::getClaimExpiresAt, now)
                .isNull(BuddyActivity::getDeletedAt));
        for (BuddyActivity activity : expired) {
            if (activities.releaseExpiredClaim(activity.getId()) == 1) {
                audit("AUTO_RELEASE_ACTIVITY_REVIEW_CLAIM", "ACTIVITY", activity.getId(), "SYSTEM",
                        Map.of("reviewStatus", activity.getReviewStatus().name(), "reviewerId", activity.getReviewerId()),
                        Map.of("reviewStatus", activity.getReviewStatus().name()),
                        "审核认领超时自动释放");
            }
        }
    }

    private void expireRecruitingActivities(Instant now) {
        List<BuddyActivity> expired = activities.selectList(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getLifecycleStatus, ActivityLifecycleStatus.RECRUITING)
                .eq(BuddyActivity::getReviewStatus, com.campusbuddies.activity.ActivityReviewStatus.APPROVED)
                .eq(BuddyActivity::getModerationStatus, com.campusbuddies.activity.ActivityModerationStatus.NORMAL)
                .lt(BuddyActivity::getApplyDeadline, now)
                .isNull(BuddyActivity::getDeletedAt));
        for (BuddyActivity activity : expired) {
            if (activities.expireRecruiting(activity.getId()) == 1) {
                conversations.updateStatus(activity.getId(), "READ_ONLY");
                audit("AUTO_EXPIRE_ACTIVITY", "ACTIVITY", activity.getId(), "SYSTEM",
                        Map.of("lifecycleStatus", ActivityLifecycleStatus.RECRUITING.name()),
                        Map.of("lifecycleStatus", ActivityLifecycleStatus.EXPIRED.name()),
                        "报名截止后自动过期");
            }
        }
    }

    private void autoCompleteTimedOutActivities(Instant now) {
        List<BuddyActivity> pending = activities.selectList(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getLifecycleStatus, ActivityLifecycleStatus.COMPLETION_PENDING)
                .isNotNull(BuddyActivity::getCompletionDeadlineAt)
                .lt(BuddyActivity::getCompletionDeadlineAt, now)
                .isNull(BuddyActivity::getDeletedAt));
        for (BuddyActivity activity : pending) {
            List<BuddyMember> activeMembers = members.findActiveMembers(activity.getId());
            boolean disputed = activeMembers.stream()
                    .anyMatch(member -> member.getCompletionStatus() == CompletionStatus.DISPUTED);
            if (disputed) {
                continue;
            }
            members.autoConfirmPending(activity.getId());
            if (activities.completeIfAllConfirmed(activity.getId()) == 1) {
                conversations.updateStatus(activity.getId(), "READ_ONLY");
                audit("AUTO_COMPLETE_ACTIVITY", "ACTIVITY", activity.getId(), "SYSTEM",
                        Map.of("lifecycleStatus", ActivityLifecycleStatus.COMPLETION_PENDING.name()),
                        Map.of("lifecycleStatus", ActivityLifecycleStatus.COMPLETED.name()),
                        "完成确认超时后自动完成");
                insertActivityStatusLog(activity.getId(), "AUTO_COMPLETE", ActivityLifecycleStatus.COMPLETION_PENDING.name(),
                        ActivityLifecycleStatus.COMPLETED.name(), "SYSTEM", "完成确认超时自动确认");
            }
        }
    }

    private void releaseExpiredReportClaims(Instant now) {
        List<ReportCase> expired = reports.selectList(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getStatus, ReportStatus.REVIEWING)
                .isNotNull(ReportCase::getClaimExpiresAt)
                .lt(ReportCase::getClaimExpiresAt, now));
        for (ReportCase report : expired) {
            if (reports.releaseExpiredClaim(report.getId()) == 1) {
                audit("AUTO_RELEASE_REPORT_REVIEW_CLAIM", "REPORT", report.getId(), "SYSTEM",
                        Map.of("status", report.getStatus().name(), "assigneeId", report.getAssigneeId()),
                        Map.of("status", ReportStatus.SUBMITTED.name()),
                        "举报认领超时自动释放");
            }
        }
    }

    private void audit(String actionName, String targetType, long targetId, String operatorRole,
                       Object before, Object after, String reason) {
        AuditLog log = new AuditLog();
        log.setOperatorId(null);
        log.setOperatorRole(operatorRole);
        log.setCampusId(null);
        log.setActionName(actionName);
        log.setTargetType(targetType);
        log.setTargetId(String.valueOf(targetId));
        log.setBeforeState(json(before));
        log.setAfterState(json(after));
        log.setReason(reason);
        log.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        audits.insert(log);
    }

    private void insertActivityStatusLog(long activityId, String actionName, String from, String to,
                                         String operatorRole, String reason) {
        SysUser operator = users.selectById(SYSTEM_OPERATOR_ID);
        if (operator == null) return;
        ActivityStatusLog entry = new ActivityStatusLog();
        entry.setActivityId(activityId);
        entry.setOperatorId(SYSTEM_OPERATOR_ID);
        entry.setDimensionName("LIFECYCLE");
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setActionName(actionName);
        entry.setReason(reason);
        entry.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        statusLogs.insert(entry);
    }

    private String json(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("审计数据序列化失败", ex);
        }
    }
}
