package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.common.RequestIdFilter;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.governance.UserBlockMapper;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import com.campusbuddies.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ApplicationService {
    public record ApplicationView(
            long id,
            long activityId,
            long applicantId,
            String applicantNickname,
            List<String> answers,
            String message,
            ApplicationStatus status,
            String decisionReason,
            int version,
            Instant createdAt) {}

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final BuddyApplicationMapper applications;
    private final BuddyActivityMapper activities;
    private final BuddyMemberMapper members;
    private final ConversationMapper conversations;
    private final ActivityStatusLogMapper statusLogs;
    private final SysUserMapper users;
    private final ObjectMapper json;
    private final NotificationService notifications;
    private final UserBlockMapper blocks;

    public ApplicationService(BuddyApplicationMapper applications,
                              BuddyActivityMapper activities,
                              BuddyMemberMapper members,
                              ConversationMapper conversations,
                              ActivityStatusLogMapper statusLogs,
                              SysUserMapper users,
                              NotificationService notifications,
                              UserBlockMapper blocks,
                              ObjectMapper json) {
        this.applications = applications;
        this.activities = activities;
        this.members = members;
        this.conversations = conversations;
        this.statusLogs = statusLogs;
        this.users = users;
        this.notifications = notifications;
        this.blocks = blocks;
        this.json = json;
    }

    @Transactional
    public ApplicationView apply(long activityId, List<String> answers, String message) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity activity = requireActivity(activityId);
        requireRecruiting(activity, principal);
        if (activity.getCreatorId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能申请自己发布的活动");
        }
        if (blocks.existsEitherDirection(principal.userId(), activity.getCreatorId()) > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "双方存在拉黑关系，不能申请该活动");
        }
        List<String> cleanAnswers = validateAnswers(answers);
        String cleanMessage = optional(message, "申请说明", 500);
        BuddyApplication application = new BuddyApplication();
        application.setActivityId(activityId);
        application.setApplicantId(principal.userId());
        application.setAnswersJson(writeJson(cleanAnswers));
        application.setMessage(cleanMessage);
        application.setStatus(ApplicationStatus.PENDING);
        application.setVersion(0);
        try {
            applications.insert(application);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_APPLICATION);
        }
        notifications.create(activity.getCreatorId(), "NEW_APPLICATION", "收到新的加入申请",
                "你的活动「" + activity.getTitle() + "」收到一条新申请", "ACTIVITY", activityId);
        return view(application);
    }

    public PageResult<ApplicationView> mine(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        IPage<BuddyApplication> result = applications.selectPage(Page.of(page, safeSize),
                new LambdaQueryWrapper<BuddyApplication>()
                        .eq(BuddyApplication::getApplicantId, principal.userId())
                        .orderByDesc(BuddyApplication::getCreatedAt));
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getTotal(), page, safeSize);
    }

    public PageResult<ApplicationView> forActivity(long activityId, int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        BuddyActivity activity = requireActivity(activityId);
        if (!activity.getCreatorId().equals(principal.userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        IPage<BuddyApplication> result = applications.selectPage(Page.of(page, safeSize),
                new LambdaQueryWrapper<BuddyApplication>()
                        .eq(BuddyApplication::getActivityId, activityId)
                        .orderByAsc(BuddyApplication::getStatus)
                        .orderByAsc(BuddyApplication::getCreatedAt));
        return new PageResult<>(result.getRecords().stream().map(this::view).toList(),
                result.getTotal(), page, safeSize);
    }

    @Transactional
    public ApplicationView decide(long applicationId, int expectedVersion, boolean accept, String reason) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyApplication application = requireApplication(applicationId);
        BuddyActivity activity = requireActivity(application.getActivityId());
        if (!activity.getCreatorId().equals(principal.userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.RECRUITING
                || activity.getReviewStatus() != ActivityReviewStatus.APPROVED
                || activity.getModerationStatus() != ActivityModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "活动当前不能处理申请");
        }
        String cleanReason = optional(reason, "处理说明", 500);
        if (!accept) {
            if (applications.reject(applicationId, expectedVersion, cleanReason) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "申请状态已变化，请刷新后重试");
            }
            log(activity.getId(), principal.userId(), "MEMBERSHIP", ApplicationStatus.PENDING.name(),
                    ApplicationStatus.REJECTED.name(), "REJECT_APPLICATION", cleanReason);
            notifications.create(application.getApplicantId(), "APPLICATION_REJECTED", "加入申请未通过",
                    "你申请的活动「" + activity.getTitle() + "」未通过", "ACTIVITY", activity.getId());
            return view(requireApplication(applicationId));
        }

        SysUser applicant = users.findByIdForUpdate(application.getApplicantId());
        if (applicant == null || applicant.getRole() != UserRole.STUDENT
                || applicant.getStatus() != UserStatus.ACTIVE
                || applicant.getVerificationStatus() != VerificationStatus.APPROVED
                || !Objects.equals(applicant.getCampusId(), activity.getCampusId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "申请人当前不符合加入条件");
        }
        if (blocks.existsEitherDirection(application.getApplicantId(), activity.getCreatorId()) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "双方存在拉黑关系，不能接受该申请");
        }

        if (applications.accept(applicationId, expectedVersion) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "申请状态已变化，请刷新后重试");
        }
        if (activities.incrementAccepted(activity.getId()) != 1) {
            throw new BusinessException(ErrorCode.CAPACITY_FULL);
        }
        BuddyMember member = new BuddyMember();
        member.setActivityId(activity.getId());
        member.setUserId(application.getApplicantId());
        member.setMemberRole(MemberRole.PARTICIPANT);
        member.setStatus(MemberStatus.ACTIVE);
        member.setCompletionStatus(CompletionStatus.PENDING);
        try {
            members.insert(member);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "该用户已经是活动成员");
        }
        conversations.insertIgnore(IdWorker.getId(), activity.getId());
        log(activity.getId(), principal.userId(), "MEMBERSHIP", ApplicationStatus.PENDING.name(),
                ApplicationStatus.ACCEPTED.name(), "ACCEPT_APPLICATION", null);
        notifications.create(application.getApplicantId(), "APPLICATION_ACCEPTED", "加入申请已通过",
                "你已加入活动「" + activity.getTitle() + "」", "ACTIVITY", activity.getId());
        return view(requireApplication(applicationId));
    }

    @Transactional
    public ApplicationView withdraw(long applicationId, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyApplication application = requireApplication(applicationId);
        if (!application.getApplicantId().equals(principal.userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        BuddyActivity activity = requireActivity(application.getActivityId());
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (application.getStatus() == ApplicationStatus.PENDING) {
            if (applications.withdrawPending(applicationId, principal.userId(), expectedVersion) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "申请状态已变化，请刷新后重试");
            }
            return view(requireApplication(applicationId));
        }
        if (application.getStatus() != ApplicationStatus.ACCEPTED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前申请不可撤销");
        }
        if (!activity.getStartAt().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "活动开始后不能自行退出，请联系发起人");
        }
        if (applications.cancelAccepted(applicationId, principal.userId(), expectedVersion) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "申请状态已变化，请刷新后重试");
        }
        if (members.markLeft(activity.getId(), principal.userId()) != 1 || activities.decrementAccepted(activity.getId()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "成员状态异常，请刷新后重试");
        }
        log(activity.getId(), principal.userId(), "MEMBERSHIP", ApplicationStatus.ACCEPTED.name(),
                ApplicationStatus.CANCELLED.name(), "LEAVE_ACTIVITY", null);
        notifications.create(activity.getCreatorId(), "MEMBER_LEFT", "参与者已退出",
                "有参与者退出活动「" + activity.getTitle() + "」", "ACTIVITY", activity.getId());
        return view(requireApplication(applicationId));
    }

    private BuddyActivity requireActivity(long id) {
        BuddyActivity activity = activities.selectOne(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getId, id).isNull(BuddyActivity::getDeletedAt));
        if (activity == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return activity;
    }

    private BuddyApplication requireApplication(long id) {
        BuddyApplication application = applications.selectById(id);
        if (application == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return application;
    }

    private void requireRecruiting(BuddyActivity activity, AuthPrincipal principal) {
        if (!Objects.equals(principal.campusId(), activity.getCampusId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (activity.getReviewStatus() != ActivityReviewStatus.APPROVED
                || activity.getLifecycleStatus() != ActivityLifecycleStatus.RECRUITING
                || activity.getModerationStatus() != ActivityModerationStatus.NORMAL
                || !activity.getApplyDeadline().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "活动当前不接受申请");
        }
        if (activity.getAcceptedCount() >= activity.getCapacity()) throw new BusinessException(ErrorCode.CAPACITY_FULL);
    }

    private ApplicationView view(BuddyApplication application) {
        SysUser user = users.selectById(application.getApplicantId());
        return new ApplicationView(application.getId(), application.getActivityId(), application.getApplicantId(),
                user == null ? "未知用户" : user.getNickname(), readJson(application.getAnswersJson()),
                application.getMessage(), application.getStatus(), application.getDecisionReason(),
                application.getVersion() == null ? 0 : application.getVersion(), application.getCreatedAt());
    }

    private void log(long activityId, long operatorId, String dimension, String from, String to,
                     String action, String reason) {
        ActivityStatusLog entry = new ActivityStatusLog();
        entry.setActivityId(activityId);
        entry.setOperatorId(operatorId);
        entry.setDimensionName(dimension);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setActionName(action);
        entry.setReason(reason);
        entry.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        statusLogs.insert(entry);
    }

    private List<String> validateAnswers(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        if (values.size() > 3) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "申请回答最多 3 项");
        return values.stream().map(value -> optional(value, "申请回答", 300)).toList();
    }

    private String writeJson(List<String> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数据序列化失败");
        }
    }

    private List<String> readJson(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return json.readValue(value, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "申请回答数据损坏");
        }
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
