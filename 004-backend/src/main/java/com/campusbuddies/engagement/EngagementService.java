package com.campusbuddies.engagement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbuddies.activity.ActivityLifecycleStatus;
import com.campusbuddies.activity.ActivityModerationStatus;
import com.campusbuddies.activity.ActivityReviewStatus;
import com.campusbuddies.activity.BuddyActivity;
import com.campusbuddies.activity.BuddyActivityMapper;
import com.campusbuddies.activity.BuddyMember;
import com.campusbuddies.activity.BuddyMemberMapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.common.PageResult;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EngagementService {
    public record FavoriteView(long activityId, String sceneName, String title, Instant startAt,
                               ActivityLifecycleStatus lifecycleStatus,
                               ActivityModerationStatus moderationStatus, boolean available) {}
    public record EvaluationView(long id, long activityId, int rating, List<String> tags, Instant createdAt) {}
    public record ReputationView(long receivedCount, double averageRating,
                                 Map<Integer, Long> distribution, List<EvaluationView> recent) {}
    public record EvaluationTarget(long userId, String nickname, boolean alreadyEvaluated) {}

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final FavoriteActivityMapper favorites;
    private final EvaluationMapper evaluations;
    private final BuddyActivityMapper activities;
    private final BuddyMemberMapper members;
    private final SysUserMapper users;
    private final ObjectMapper json;

    public EngagementService(FavoriteActivityMapper favorites, EvaluationMapper evaluations,
                             BuddyActivityMapper activities, BuddyMemberMapper members,
                             SysUserMapper users, ObjectMapper json) {
        this.favorites = favorites;
        this.evaluations = evaluations;
        this.activities = activities;
        this.members = members;
        this.users = users;
        this.json = json;
    }

    @Transactional
    public void favorite(long activityId) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        BuddyActivity activity = requireActivity(activityId);
        if (!principal.campusId().equals(activity.getCampusId())
                || activity.getReviewStatus() != ActivityReviewStatus.APPROVED
                || activity.getModerationStatus() != ActivityModerationStatus.NORMAL) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        favorites.insertIgnore(principal.userId(), activityId);
    }

    @Transactional
    public void unfavorite(long activityId) {
        favorites.delete(SecuritySupport.current().userId(), activityId);
    }

    public PageResult<FavoriteView> favorites(int page, int size) {
        long userId = SecuritySupport.current().userId();
        int safeSize = PageResult.safeSize(size);
        if (page < 1) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1");
        List<FavoriteView> records = favorites.findPage(userId, (long) (page - 1) * safeSize, safeSize)
                .stream().map(this::favoriteView).toList();
        return new PageResult<>(records, favorites.count(userId), page, safeSize);
    }

    @Transactional
    public EvaluationView evaluate(long activityId, long revieweeId, int rating,
                                   List<String> tags, String privateNote) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireVerifiedStudent(principal);
        if (principal.userId() == revieweeId) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "不能评价自己");
        BuddyActivity activity = requireActivity(activityId);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "活动完成后才能评价");
        }
        if (members.isActiveMember(activityId, principal.userId()) == 0
                || members.isActiveMember(activityId, revieweeId) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能评价同一活动的有效成员");
        }
        if (rating < 1 || rating > 5) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "评分必须在 1 到 5 之间");
        List<String> cleanTags = normalizeTags(tags);
        String cleanNote = optional(privateNote, 500);
        Evaluation evaluation = new Evaluation();
        evaluation.setActivityId(activityId);
        evaluation.setReviewerId(principal.userId());
        evaluation.setRevieweeId(revieweeId);
        evaluation.setRating(rating);
        evaluation.setTagsJson(writeJson(cleanTags));
        evaluation.setPrivateNote(cleanNote);
        try {
            evaluations.insert(evaluation);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.CONFLICT, "同一活动中不能重复评价该成员");
        }
        return view(evaluation);
    }

    public List<EvaluationTarget> targets(long activityId) {
        AuthPrincipal principal = SecuritySupport.current();
        BuddyActivity activity = requireActivity(activityId);
        SecuritySupport.requireCampus(principal, activity.getCampusId());
        if (activity.getLifecycleStatus() != ActivityLifecycleStatus.COMPLETED
                || members.isActiveMember(activityId, principal.userId()) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        List<Long> evaluated = evaluations.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getActivityId, activityId)
                .eq(Evaluation::getReviewerId, principal.userId()))
                .stream().map(Evaluation::getRevieweeId).toList();
        return members.findActiveMembers(activityId).stream()
                .filter(member -> !member.getUserId().equals(principal.userId()))
                .map(member -> {
                    SysUser user = users.selectById(member.getUserId());
                    return new EvaluationTarget(member.getUserId(), user == null ? "未知用户" : user.getNickname(),
                            evaluated.contains(member.getUserId()));
                }).toList();
    }

    public ReputationView reputation() {
        long userId = SecuritySupport.current().userId();
        List<Evaluation> received = evaluations.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getRevieweeId, userId)
                .orderByDesc(Evaluation::getCreatedAt)
                .last("LIMIT 50"));
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) distribution.put(rating, 0L);
        received.forEach(item -> distribution.compute(item.getRating(), (key, count) -> count == null ? 1 : count + 1));
        return new ReputationView(evaluations.receivedCount(userId), evaluations.averageRating(userId),
                distribution, received.stream().limit(10).map(this::view).toList());
    }

    private BuddyActivity requireActivity(long id) {
        BuddyActivity activity = activities.selectOne(new LambdaQueryWrapper<BuddyActivity>()
                .eq(BuddyActivity::getId, id).isNull(BuddyActivity::getDeletedAt));
        if (activity == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return activity;
    }

    private FavoriteView favoriteView(BuddyActivity activity) {
        boolean available = activity.getReviewStatus() == ActivityReviewStatus.APPROVED
                && activity.getLifecycleStatus() == ActivityLifecycleStatus.RECRUITING
                && activity.getModerationStatus() == ActivityModerationStatus.NORMAL
                && activity.getApplyDeadline().isAfter(Instant.now());
        return new FavoriteView(activity.getId(), activity.getSceneName(), activity.getTitle(), activity.getStartAt(),
                activity.getLifecycleStatus(), activity.getModerationStatus(), available);
    }

    private EvaluationView view(Evaluation evaluation) {
        return new EvaluationView(evaluation.getId(), evaluation.getActivityId(), evaluation.getRating(),
                readJson(evaluation.getTagsJson()), evaluation.getCreatedAt());
    }

    private List<String> normalizeTags(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        if (values.size() > 5) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "评价标签最多 5 个");
        return values.stream().map(value -> {
            String cleaned = optional(value, 20);
            if (cleaned == null) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "评价标签不能为空");
            return cleaned;
        }).distinct().toList();
    }

    private String writeJson(List<String> value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评价数据序列化失败");
        }
    }

    private List<String> readJson(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try {
            return json.readValue(value, STRING_LIST);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评价数据损坏");
        }
    }

    private static String optional(String value, int max) {
        if (!StringUtils.hasText(value)) return null;
        String cleaned = value.trim();
        if (cleaned.codePointCount(0, cleaned.length()) > max) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "内容不能超过 " + max + " 字");
        }
        return cleaned;
    }
}
