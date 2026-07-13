package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.common.RequestIdFilter;
import com.campusbuddies.governance.GovernanceService;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.file.ActivityMediaMapper;
import com.campusbuddies.governance.UserBlockMapper;
import com.campusbuddies.notification.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ActivityService {
    public record Command(
            String sceneName,
            String title,
            String description,
            MeetingMode meetingMode,
            String publicLocation,
            String memberLocationDetail,
            String joinRequirement,
            List<String> joinQuestions,
            Instant startAt,
            Instant endAt,
            Instant applyDeadline,
            Integer capacity,
            List<String> tags) {}

    public record ActivityView(
            long id,
            long campusId,
            long creatorId,
            String sceneName,
            String title,
            String description,
            MeetingMode meetingMode,
            String publicLocation,
            String memberLocationDetail,
            String joinRequirement,
            List<String> joinQuestions,
            Instant startAt,
            Instant endAt,
            Instant applyDeadline,
            int capacity,
            int acceptedCount,
            ActivityReviewStatus reviewStatus,
            ActivityLifecycleStatus lifecycleStatus,
            ActivityModerationStatus moderationStatus,
            Long reviewerId,
            Instant claimExpiresAt,
            String reviewReason,
            Instant completionDeadlineAt,
            int version,
            List<String> tags,
            List<Long> mediaIds,
            Instant createdAt,
            Instant updatedAt) {}

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final BuddyActivityMapper activities;
    private final ActivityTagMapper tags;
    private final ActivityTagRelationMapper tagRelations;
    private final BuddyMemberMapper members;
    private final ConversationMapper conversations;
    private final ActivityMediaMapper activityMedia;
    private final ActivityStatusLogMapper statusLogs;
    private final NotificationService notifications;
    private final UserBlockMapper blocks;
    private final GovernanceService governance;
    private final ObjectMapper json;

    public ActivityService(BuddyActivityMapper activities,
                           ActivityTagMapper tags,
                           ActivityTagRelationMapper tagRelations,
                           BuddyMemberMapper members,
                           ConversationMapper conversations,
                           ActivityMediaMapper activityMedia,
                           ActivityStatusLogMapper statusLogs,
                           NotificationService notifications,
                           UserBlockMapper blocks,
                           GovernanceService governance,
                           ObjectMapper json) {
        this.activities = activities;
        this.tags = tags;
        this.tagRelations = tagRelations;
        this.members = members;
        this.conversations = conversations;
        this.activityMedia = activityMedia;
        this.statusLogs = statusLogs;
        this.notifications = notifications;
        this.blocks = blocks;
        this.governance = governance;
        this.json = json;
    }

    @Transactional
    public ActivityView create(Command command) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        if (principal.campusId() == null) throw new BusinessException(ErrorCode.FORBIDDEN, "账号未绑定校园");
        Validated validated = validate(command);

        BuddyActivity activity = new BuddyActivity();
        activity.setCampusId(principal.campusId());
        activity.setCreatorId(principal.userId());
        apply(activity, validated);
        activity.setAcceptedCount(0);
        activity.setReviewStatus(ActivityReviewStatus.NOT_SUBMITTED);
        activity.setLifecycleStatus(ActivityLifecycleStatus.DRAFT);
        activity.setModerationStatus(ActivityModerationStatus.NORMAL);
        activity.setVersion(0);
        activities.insert(activity);

        BuddyMember creator = new BuddyMember();
        creator.setActivityId(activity.getId());
        creator.setUserId(principal.userId());
        creator.setMemberRole(MemberRole.CREATOR);
        creator.setStatus(MemberStatus.ACTIVE);
        creator.setCompletionStatus(CompletionStatus.PENDING);
        members.insert(creator);
        replaceTags(activity.getId(), activity.getCampusId(), validated.tags());
        log(activity.getId(), principal.userId(), "LIFECYCLE", null, ActivityLifecycleStatus.DRAFT.name(), "CREATE", null);
        return view(activity, true);
    }

    @Transactional
    public ActivityView update(long id, int expectedVersion, Command command) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity current = requireActivity(id);
        requireOwner(current, principal);
        if (current.getAcceptedCount() != null && current.getAcceptedCount() > 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "已有成员加入，核心信息不可修改");
        }
        if (current.getLifecycleStatus() != ActivityLifecycleStatus.DRAFT
                || (current.getReviewStatus() != ActivityReviewStatus.NOT_SUBMITTED
                    && current.getReviewStatus() != ActivityReviewStatus.REJECTED)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前状态不可编辑");
        }
        Validated validated = validate(command);
        LambdaUpdateWrapper<BuddyActivity> update = new LambdaUpdateWrapper<BuddyActivity>()
                .eq(BuddyActivity::getId, id)
                .eq(BuddyActivity::getCreatorId, principal.userId())
                .eq(BuddyActivity::getVersion, expectedVersion)
                .eq(BuddyActivity::getLifecycleStatus, ActivityLifecycleStatus.DRAFT)
                .in(BuddyActivity::getReviewStatus, ActivityReviewStatus.NOT_SUBMITTED, ActivityReviewStatus.REJECTED)
                .isNull(BuddyActivity::getDeletedAt)
                .set(BuddyActivity::getSceneName, validated.sceneName())
                .set(BuddyActivity::getTitle, validated.title())
                .set(BuddyActivity::getDescription, validated.description())
                .set(BuddyActivity::getMeetingMode, validated.meetingMode())
                .set(BuddyActivity::getPublicLocation, validated.publicLocation())
                .set(BuddyActivity::getMemberLocationDetail, validated.memberLocationDetail())
                .set(BuddyActivity::getJoinRequirement, validated.joinRequirement())
                .set(BuddyActivity::getJoinQuestionsJson, writeJson(validated.joinQuestions()))
                .set(BuddyActivity::getStartAt, validated.startAt())
                .set(BuddyActivity::getEndAt, validated.endAt())
                .set(BuddyActivity::getApplyDeadline, validated.applyDeadline())
                .set(BuddyActivity::getCapacity, validated.capacity())
                .set(BuddyActivity::getReviewStatus, ActivityReviewStatus.NOT_SUBMITTED)
                .set(BuddyActivity::getReviewReason, null)
                .setSql("version = version + 1");
        if (activities.update(null, update) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动已被修改，请刷新后重试");
        }
        replaceTags(id, current.getCampusId(), validated.tags());
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView submit(long id, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity current = requireActivity(id);
        requireOwner(current, principal);
        if (!current.getApplyDeadline().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "申请截止时间必须晚于当前时间");
        }
        if (activities.submitForReview(id, principal.userId(), expectedVersion) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "活动状态已变化，请刷新后重试");
        }
        log(id, principal.userId(), "REVIEW", current.getReviewStatus().name(),
                ActivityReviewStatus.PENDING.name(), "SUBMIT_REVIEW", null);
        return view(requireActivity(id), true);
    }

    public PageResult<ActivityView> discover(int page, int size, String keyword, MeetingMode mode) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        String queryText = clean(keyword);
        if (queryText != null && codePoints(queryText) > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "搜索词不能超过 50 字");
        }
        LambdaQueryWrapper<BuddyActivity> query = new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getCampusId, principal.campusId())
                .eq(BuddyActivity::getReviewStatus, ActivityReviewStatus.APPROVED)
                .eq(BuddyActivity::getLifecycleStatus, ActivityLifecycleStatus.RECRUITING)
                .eq(BuddyActivity::getModerationStatus, ActivityModerationStatus.NORMAL)
                .gt(BuddyActivity::getApplyDeadline, Instant.now())
                .isNull(BuddyActivity::getDeletedAt)
                .notExists("""
                        SELECT 1 FROM user_block ub
                         WHERE (ub.blocker_id = {0} AND ub.blocked_id = creator_id)
                            OR (ub.blocker_id = creator_id AND ub.blocked_id = {0})
                        """, principal.userId())
                .orderByAsc(BuddyActivity::getStartAt)
                .orderByDesc(BuddyActivity::getCreatedAt);
        if (mode != null) query.eq(BuddyActivity::getMeetingMode, mode);
        if (queryText != null) {
            query.and(group -> group.like(BuddyActivity::getTitle, queryText)
                    .or().like(BuddyActivity::getSceneName, queryText)
                    .or().like(BuddyActivity::getDescription, queryText));
        }
        IPage<BuddyActivity> result = activities.selectPage(Page.of(page, safeSize), query);
        List<ActivityView> records = result.getRecords().stream().map(a -> view(a, false)).toList();
        return new PageResult<>(records, result.getTotal(), page, safeSize);
    }

    public PageResult<ActivityView> mine(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        IPage<BuddyActivity> result = activities.selectPage(Page.of(page, safeSize),
                new LambdaQueryWrapper<BuddyActivity>()
                        .eq(BuddyActivity::getCreatorId, principal.userId())
                        .isNull(BuddyActivity::getDeletedAt)
                        .orderByDesc(BuddyActivity::getCreatedAt));
        return new PageResult<>(result.getRecords().stream().map(a -> view(a, true)).toList(),
                result.getTotal(), page, safeSize);
    }

    public ActivityView detail(long id) {
        AuthPrincipal principal = SecuritySupport.current();
        BuddyActivity activity = requireActivity(id);
        boolean owner = activity.getCreatorId().equals(principal.userId());
        boolean reviewer = principal.isReviewer();
        if (reviewer) SecuritySupport.requireCampus(principal, activity.getCampusId());
        boolean publicVisible = principal.isVerifiedStudent()
                && Objects.equals(principal.campusId(), activity.getCampusId())
                && blocks.existsEitherDirection(principal.userId(), activity.getCreatorId()) == 0
                && activity.getReviewStatus() == ActivityReviewStatus.APPROVED
                && activity.getLifecycleStatus() == ActivityLifecycleStatus.RECRUITING
                && activity.getModerationStatus() == ActivityModerationStatus.NORMAL;
        boolean member = members.isActiveMember(id, principal.userId()) > 0;
        if (!owner && !reviewer && !publicVisible && !member) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return view(activity, owner || reviewer || member);
    }

    public PageResult<ActivityView> reviewQueue(int page, int size) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        LambdaQueryWrapper<BuddyActivity> query = new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getReviewStatus, ActivityReviewStatus.PENDING)
                .isNull(BuddyActivity::getDeletedAt)
                .orderByAsc(BuddyActivity::getCreatedAt);
        if (!principal.isPlatformAdmin()) query.eq(BuddyActivity::getCampusId, principal.campusId());
        IPage<BuddyActivity> result = activities.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(a -> view(a, true)).toList(),
                result.getTotal(), page, safeSize);
    }

    public PageResult<ActivityView> operations(int page, int size, String keyword,
                                                ActivityReviewStatus reviewStatus,
                                                ActivityLifecycleStatus lifecycleStatus,
                                                ActivityModerationStatus moderationStatus,
                                                MeetingMode meetingMode) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        String queryText = clean(keyword);
        if (queryText != null && codePoints(queryText) > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "搜索词不能超过 50 字");
        }
        LambdaQueryWrapper<BuddyActivity> query = new LambdaQueryWrapper<BuddyActivity>()
                .isNull(BuddyActivity::getDeletedAt)
                .orderByDesc(BuddyActivity::getCreatedAt)
                .orderByDesc(BuddyActivity::getId);
        if (!principal.isPlatformAdmin()) query.eq(BuddyActivity::getCampusId, principal.campusId());
        if (reviewStatus != null) query.eq(BuddyActivity::getReviewStatus, reviewStatus);
        if (lifecycleStatus != null) query.eq(BuddyActivity::getLifecycleStatus, lifecycleStatus);
        if (moderationStatus != null) query.eq(BuddyActivity::getModerationStatus, moderationStatus);
        if (meetingMode != null) query.eq(BuddyActivity::getMeetingMode, meetingMode);
        if (queryText != null) {
            query.and(group -> group.like(BuddyActivity::getTitle, queryText)
                    .or().like(BuddyActivity::getSceneName, queryText)
                    .or().like(BuddyActivity::getDescription, queryText));
        }
        IPage<BuddyActivity> result = activities.selectPage(Page.of(page, safeSize), query);
        return new PageResult<>(result.getRecords().stream().map(activity -> view(activity, false, false)).toList(),
                result.getTotal(), page, safeSize);
    }

    @Transactional
    public ActivityView claim(long id, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        BuddyActivity activity = requireActivity(id);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (activities.claim(id, expectedVersion, principal.userId(), Instant.now().plus(15, ChronoUnit.MINUTES)) != 1) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_CLAIMED);
        }
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView decide(long id, int expectedVersion, boolean approve, String reason) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        BuddyActivity activity = requireActivity(id);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        String cleanReason = clean(reason);
        if (!approve && (cleanReason == null || codePoints(cleanReason) > 500)) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "驳回时必须填写 1 到 500 字理由");
        }
        int changed = approve
                ? activities.approve(id, expectedVersion, principal.userId())
                : activities.reject(id, expectedVersion, principal.userId(), cleanReason);
        if (changed != 1) throw new BusinessException(ErrorCode.CONFLICT, "审核状态或认领已变化，请刷新后重试");
        ActivityReviewStatus target = approve ? ActivityReviewStatus.APPROVED : ActivityReviewStatus.REJECTED;
        log(id, principal.userId(), "REVIEW", ActivityReviewStatus.PENDING.name(), target.name(),
                approve ? "APPROVE" : "REJECT", cleanReason);
        if (approve) {
            log(id, principal.userId(), "LIFECYCLE", ActivityLifecycleStatus.DRAFT.name(),
                    ActivityLifecycleStatus.RECRUITING.name(), "PUBLISH", null);
        }
        notifications.create(activity.getCreatorId(), approve ? "ACTIVITY_APPROVED" : "ACTIVITY_REJECTED",
                approve ? "活动已通过审核" : "活动未通过审核",
                approve ? "你的活动已发布到搭子广场" : "请根据审核理由修改后重新提交",
                "ACTIVITY", id);
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView start(long id, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity current = requireActivity(id);
        requireOwner(current, principal);
        if (activities.start(id, principal.userId(), expectedVersion) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "活动需已通过审核、有参与者且距离开始不超过 30 分钟");
        }
        log(id, principal.userId(), "LIFECYCLE", ActivityLifecycleStatus.RECRUITING.name(),
                ActivityLifecycleStatus.IN_PROGRESS.name(), "START", null);
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView requestCompletion(long id, int expectedVersion) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity current = requireActivity(id);
        requireOwner(current, principal);
        if (activities.requestCompletion(id, principal.userId(), expectedVersion) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前活动不能发起完成确认");
        }
        log(id, principal.userId(), "LIFECYCLE", ActivityLifecycleStatus.IN_PROGRESS.name(),
                ActivityLifecycleStatus.COMPLETION_PENDING.name(), "REQUEST_COMPLETION", null);
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView confirmCompletion(long id, boolean disputed) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity activity = requireActivity(id);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.COMPLETION_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "活动尚未进入完成确认");
        }
        CompletionStatus target = disputed ? CompletionStatus.DISPUTED : CompletionStatus.CONFIRMED;
        int changed = members.setCompletionStatus(id, principal.userId(), target);
        if (changed != 1) {
            BuddyMember existing = members.findActiveMembers(id).stream()
                    .filter(member -> member.getUserId().equals(principal.userId()))
                    .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
            if (existing.getCompletionStatus() != target) {
                throw new BusinessException(ErrorCode.CONFLICT, "完成确认已经提交，不能重复修改");
            }
        } else {
            log(id, principal.userId(), "COMPLETION", CompletionStatus.PENDING.name(), target.name(),
                    disputed ? "DISPUTE_COMPLETION" : "CONFIRM_COMPLETION", null);
        }
        if (disputed) {
            governance.createCompletionDisputeCase(principal.userId(), id, "成员对完成确认存在异议");
        }
        if (!disputed && activities.completeIfAllConfirmed(id) == 1) {
            conversations.updateStatus(id, "READ_ONLY");
            log(id, principal.userId(), "LIFECYCLE", ActivityLifecycleStatus.COMPLETION_PENDING.name(),
                    ActivityLifecycleStatus.COMPLETED.name(), "COMPLETE", null);
        }
        return view(requireActivity(id), true);
    }

    @Transactional
    public ActivityView cancel(long id, int expectedVersion, String reason) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity current = requireActivity(id);
        requireOwner(current, principal);
        String cleanReason = optional(reason, "取消原因", 500);
        if (current.getAcceptedCount() != null && current.getAcceptedCount() > 0 && cleanReason == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "已有参与者时必须填写取消原因");
        }
        if (activities.cancel(id, principal.userId(), expectedVersion, cleanReason) != 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "当前活动不能取消");
        }
        conversations.updateStatus(id, "READ_ONLY");
        for (BuddyMember member : members.findActiveMembers(id)) {
            if (!member.getUserId().equals(principal.userId())) {
                notifications.create(member.getUserId(), "ACTIVITY_CANCELLED", "活动已取消",
                        current.getTitle() + " 已由发起人取消", "ACTIVITY", id);
            }
        }
        log(id, principal.userId(), "LIFECYCLE", current.getLifecycleStatus().name(),
                ActivityLifecycleStatus.CANCELLED.name(), "CANCEL", cleanReason);
        return view(requireActivity(id), true);
    }

    private BuddyActivity requireActivity(long id) {
        BuddyActivity activity = activities.selectOne(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getId, id).isNull(BuddyActivity::getDeletedAt));
        if (activity == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return activity;
    }

    private void requireOwner(BuddyActivity activity, AuthPrincipal principal) {
        if (!activity.getCreatorId().equals(principal.userId())) throw new BusinessException(ErrorCode.FORBIDDEN);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
    }

    private record Validated(
            String sceneName, String title, String description, MeetingMode meetingMode,
            String publicLocation, String memberLocationDetail, String joinRequirement,
            List<String> joinQuestions, Instant startAt, Instant endAt, Instant applyDeadline,
            int capacity, List<String> tags) {}

    private Validated validate(Command input) {
        if (input == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        String scene = required(input.sceneName(), "场景", 2, 20);
        String title = required(input.title(), "标题", 5, 50);
        String description = required(input.description(), "描述", 20, 1000);
        if (input.meetingMode() == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "请选择活动方式");
        String publicLocation = optional(input.publicLocation(), "公开地点", 120);
        String memberLocation = optional(input.memberLocationDetail(), "成员地点", 255);
        if (input.meetingMode() != MeetingMode.ONLINE && publicLocation == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "线下或混合活动必须填写公开地点");
        }
        String requirement = optional(input.joinRequirement(), "加入要求", 500);
        List<String> questions = normalizeItems(input.joinQuestions(), "申请问题", 3, 2, 100);
        List<String> normalizedTags = normalizeItems(input.tags(), "标签", 5, 2, 12);
        if (input.startAt() == null || input.endAt() == null || input.applyDeadline() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "活动时间不能为空");
        }
        if (!input.endAt().isAfter(input.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "结束时间必须晚于开始时间");
        }
        if (input.applyDeadline().isAfter(input.startAt())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "申请截止时间不能晚于开始时间");
        }
        if (!input.applyDeadline().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "申请截止时间必须晚于当前时间");
        }
        if (input.capacity() == null || input.capacity() < 2 || input.capacity() > 50) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "参与者名额必须在 2 到 50 之间");
        }
        return new Validated(scene, title, description, input.meetingMode(), publicLocation, memberLocation,
                requirement, questions, input.startAt(), input.endAt(), input.applyDeadline(),
                input.capacity(), normalizedTags);
    }

    private void apply(BuddyActivity activity, Validated input) {
        activity.setSceneName(input.sceneName());
        activity.setTitle(input.title());
        activity.setDescription(input.description());
        activity.setMeetingMode(input.meetingMode());
        activity.setPublicLocation(input.publicLocation());
        activity.setMemberLocationDetail(input.memberLocationDetail());
        activity.setJoinRequirement(input.joinRequirement());
        activity.setJoinQuestionsJson(writeJson(input.joinQuestions()));
        activity.setStartAt(input.startAt());
        activity.setEndAt(input.endAt());
        activity.setApplyDeadline(input.applyDeadline());
        activity.setCapacity(input.capacity());
    }

    private void replaceTags(long activityId, long campusId, List<String> names) {
        tagRelations.deleteByActivity(activityId);
        for (String name : names) {
            String normalized = normalizeTag(name);
            long candidateId = IdWorker.getId();
            tags.insertIgnore(candidateId, campusId, name, normalized);
            Long tagId = tags.findId(campusId, normalized);
            if (tagId == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "标签保存失败");
            tagRelations.insertIgnore(activityId, tagId);
        }
    }

    private ActivityView view(BuddyActivity activity, boolean includeMemberLocation) {
        return view(activity, includeMemberLocation, true);
    }

    private ActivityView view(BuddyActivity activity, boolean includeMemberLocation, boolean includeRelations) {
        return new ActivityView(
                activity.getId(), activity.getCampusId(), activity.getCreatorId(), activity.getSceneName(),
                activity.getTitle(), activity.getDescription(), activity.getMeetingMode(), activity.getPublicLocation(),
                includeMemberLocation ? activity.getMemberLocationDetail() : null,
                activity.getJoinRequirement(), readQuestions(activity.getJoinQuestionsJson()), activity.getStartAt(),
                activity.getEndAt(), activity.getApplyDeadline(), activity.getCapacity(), activity.getAcceptedCount(),
                activity.getReviewStatus(), activity.getLifecycleStatus(), activity.getModerationStatus(),
                activity.getReviewerId(), activity.getClaimExpiresAt(), activity.getReviewReason(),
                activity.getCompletionDeadlineAt(),
                activity.getVersion(), includeRelations ? tagRelations.findNames(activity.getId()) : List.of(),
                includeRelations ? activityMedia.findFileIds(activity.getId()) : List.of(),
                activity.getCreatedAt(), activity.getUpdatedAt());
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

    private String writeJson(List<String> value) {
        try {
            return json.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数据序列化失败");
        }
    }

    private List<String> readQuestions(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return json.readValue(value, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "活动问题数据损坏");
        }
    }

    private static List<String> normalizeItems(List<String> input, String label, int maxItems, int minLength, int maxLength) {
        if (input == null || input.isEmpty()) return List.of();
        if (input.size() > maxItems) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "最多 " + maxItems + " 个");
        Map<String, String> unique = new LinkedHashMap<>();
        for (String raw : input) {
            String value = required(raw, label, minLength, maxLength);
            String key = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, value);
        }
        return List.copyOf(unique.values());
    }

    private static String normalizeTag(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String label, int min, int max) {
        String cleaned = clean(value);
        int length = cleaned == null ? 0 : codePoints(cleaned);
        if (length < min || length > max) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "长度必须在 " + min + " 到 " + max + " 字之间");
        }
        return cleaned;
    }

    private static String optional(String value, String label, int max) {
        String cleaned = clean(value);
        if (cleaned != null && codePoints(cleaned) > max) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, label + "不能超过 " + max + " 字");
        }
        return cleaned;
    }

    private static String clean(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private static int codePoints(String value) {
        return value.codePointCount(0, value.length());
    }
}
