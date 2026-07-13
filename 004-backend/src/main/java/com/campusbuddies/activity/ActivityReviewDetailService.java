package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbuddies.campus.Campus;
import com.campusbuddies.campus.CampusMapper;
import com.campusbuddies.file.ActivityMediaMapper;
import com.campusbuddies.file.FileObject;
import com.campusbuddies.file.FileObjectMapper;
import com.campusbuddies.governance.AuditLog;
import com.campusbuddies.governance.AuditLogMapper;
import com.campusbuddies.governance.ModerationAction;
import com.campusbuddies.governance.ModerationActionMapper;
import com.campusbuddies.governance.ReportCase;
import com.campusbuddies.governance.ReportCaseMapper;
import com.campusbuddies.governance.ReportTargetType;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Read model used by the reviewer workspace; write and state-transition rules stay in ActivityService. */
@Service
public class ActivityReviewDetailService {
    public record PersonSummary(long id, String nickname, String gradeName, String majorName, Long avatarFileId) {}

    public record ApplicationSummary(long id, PersonSummary applicant, List<String> answers, String message,
                                     ApplicationStatus status, String decisionReason, int version, Instant createdAt) {}

    public record ParticipantSummary(PersonSummary user, MemberRole memberRole, MemberStatus status,
                                     CompletionStatus completionStatus, Instant joinedAt, Instant leftAt) {}

    public record MediaSummary(long id, String originalName, String contentType, long byteSize,
                               Integer width, Integer height, com.campusbuddies.file.FileStatus status,
                               int sortOrder, Instant createdAt) {}

    public record TimelineEntry(String source, String dimension, String actionName, String fromStatus,
                                String toStatus, String reason, String operatorName, String operatorRole,
                                Instant createdAt) {}

    public record DetailView(ActivityService.ActivityView activity, String campusName, PersonSummary creator,
                             List<ApplicationSummary> applications, List<ParticipantSummary> participants,
                             List<MediaSummary> media, List<TimelineEntry> timeline) {}

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ActivityService activities;
    private final BuddyApplicationMapper applications;
    private final BuddyMemberMapper members;
    private final ActivityMediaMapper activityMedia;
    private final FileObjectMapper files;
    private final SysUserMapper users;
    private final ActivityStatusLogMapper statusLogs;
    private final AuditLogMapper audits;
    private final ReportCaseMapper reports;
    private final ModerationActionMapper moderationActions;
    private final CampusMapper campuses;
    private final ObjectMapper json;

    public ActivityReviewDetailService(ActivityService activities,
                                       BuddyApplicationMapper applications,
                                       BuddyMemberMapper members,
                                       ActivityMediaMapper activityMedia,
                                       FileObjectMapper files,
                                       SysUserMapper users,
                                       ActivityStatusLogMapper statusLogs,
                                       AuditLogMapper audits,
                                       ReportCaseMapper reports,
                                       ModerationActionMapper moderationActions,
                                       CampusMapper campuses,
                                       ObjectMapper json) {
        this.activities = activities;
        this.applications = applications;
        this.members = members;
        this.activityMedia = activityMedia;
        this.files = files;
        this.users = users;
        this.statusLogs = statusLogs;
        this.audits = audits;
        this.reports = reports;
        this.moderationActions = moderationActions;
        this.campuses = campuses;
        this.json = json;
    }

    public DetailView detail(long activityId) {
        AuthPrincipal principal = SecuritySupport.current();
        SecuritySupport.requireReviewer(principal);
        ActivityService.ActivityView activity = activities.detail(activityId);
        // ActivityService.detail performs the same campus check for reviewers. Keep this guard here as
        // well so a future base-view change cannot accidentally widen this endpoint's data scope.
        SecuritySupport.requireCampus(principal, activity.campusId());

        List<BuddyApplication> applicationRows = applications.selectList(new LambdaQueryWrapper<BuddyApplication>()
                .eq(BuddyApplication::getActivityId, activityId)
                .orderByAsc(BuddyApplication::getCreatedAt));
        List<BuddyMember> memberRows = members.selectList(new LambdaQueryWrapper<BuddyMember>()
                .eq(BuddyMember::getActivityId, activityId)
                .orderByAsc(BuddyMember::getJoinedAt));
        List<ActivityStatusLog> statusRows = statusRows(activityId);
        List<AuditLog> auditRows = auditRows(activityId);
        List<ReportCase> reportRows = reportRows(activityId);
        List<ModerationAction> actionRows = actionRows(activityId);
        List<AuditLog> governanceAuditRows = governanceAuditRows(reportRows);
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        userIds.add(activity.creatorId());
        applicationRows.forEach(row -> userIds.add(row.getApplicantId()));
        memberRows.forEach(row -> userIds.add(row.getUserId()));
        statusRows.forEach(row -> addOperator(userIds, row.getOperatorId()));
        auditRows.forEach(row -> addOperator(userIds, row.getOperatorId()));
        reportRows.forEach(row -> addOperator(userIds, row.getReporterId()));
        actionRows.forEach(row -> addOperator(userIds, row.getOperatorId()));
        governanceAuditRows.forEach(row -> addOperator(userIds, row.getOperatorId()));
        Map<Long, SysUser> userMap = loadUsers(userIds);

        Campus campus = campuses.selectById(activity.campusId());
        List<Long> fileIds = activityMedia.findFileIds(activityId);
        Map<Long, FileObject> fileMap = loadFiles(fileIds);

        return new DetailView(
                activity,
                campus == null ? null : campus.getName(),
                person(activity.creatorId(), userMap),
                applicationRows.stream().map(row -> application(row, userMap)).toList(),
                memberRows.stream().map(row -> participant(row, userMap)).toList(),
                fileIds.stream().map(fileMap::get).filter(Objects::nonNull).map(this::media).toList(),
                 timeline(statusRows, auditRows, reportRows, actionRows, governanceAuditRows, userMap));
    }

    private List<ActivityStatusLog> statusRows(long activityId) {
        return statusLogs.selectList(new LambdaQueryWrapper<ActivityStatusLog>()
                .eq(ActivityStatusLog::getActivityId, activityId)
                .orderByAsc(ActivityStatusLog::getCreatedAt)
                .orderByAsc(ActivityStatusLog::getId));
    }

    private List<AuditLog> auditRows(long activityId) {
        return audits.selectList(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTargetType, "ACTIVITY")
                .eq(AuditLog::getTargetId, String.valueOf(activityId))
                .orderByAsc(AuditLog::getCreatedAt)
                .orderByAsc(AuditLog::getId));
    }

    private List<ReportCase> reportRows(long activityId) {
        return reports.selectList(new LambdaQueryWrapper<ReportCase>()
                .eq(ReportCase::getTargetType, ReportTargetType.ACTIVITY)
                .eq(ReportCase::getTargetId, activityId)
                .orderByAsc(ReportCase::getCreatedAt)
                .orderByAsc(ReportCase::getId));
    }

    private List<ModerationAction> actionRows(long activityId) {
        return moderationActions.selectList(new LambdaQueryWrapper<ModerationAction>()
                .eq(ModerationAction::getTargetType, ReportTargetType.ACTIVITY)
                .eq(ModerationAction::getTargetId, activityId)
                .orderByAsc(ModerationAction::getCreatedAt)
                .orderByAsc(ModerationAction::getId));
    }

    private List<AuditLog> governanceAuditRows(List<ReportCase> reportRows) {
        if (reportRows.isEmpty()) return List.of();
        return audits.selectList(new LambdaQueryWrapper<AuditLog>()
                .eq(AuditLog::getTargetType, "REPORT")
                .in(AuditLog::getTargetId, reportRows.stream().map(row -> String.valueOf(row.getId())).toList())
                .orderByAsc(AuditLog::getCreatedAt)
                .orderByAsc(AuditLog::getId));
    }

    private void addOperator(LinkedHashSet<Long> userIds, Long operatorId) {
        if (operatorId != null) userIds.add(operatorId);
    }

    private Map<Long, SysUser> loadUsers(LinkedHashSet<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return users.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds))
                .stream().collect(HashMap::new, (map, user) -> map.put(user.getId(), user), Map::putAll);
    }

    private Map<Long, FileObject> loadFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return Map.of();
        return files.selectList(new LambdaQueryWrapper<FileObject>()
                        .in(FileObject::getId, fileIds)
                        .isNull(FileObject::getDeletedAt))
                .stream().collect(HashMap::new, (map, file) -> map.put(file.getId(), file), Map::putAll);
    }

    private PersonSummary person(long userId, Map<Long, SysUser> userMap) {
        SysUser user = userMap.get(userId);
        if (user == null) return new PersonSummary(userId, "未知用户", null, null, null);
        return new PersonSummary(userId, user.getNickname(), user.getGradeName(), user.getMajorName(), user.getAvatarFileId());
    }

    private ApplicationSummary application(BuddyApplication row, Map<Long, SysUser> userMap) {
        return new ApplicationSummary(row.getId(), person(row.getApplicantId(), userMap), readList(row.getAnswersJson()),
                row.getMessage(), row.getStatus(), row.getDecisionReason(), row.getVersion() == null ? 0 : row.getVersion(),
                row.getCreatedAt());
    }

    private ParticipantSummary participant(BuddyMember row, Map<Long, SysUser> userMap) {
        return new ParticipantSummary(person(row.getUserId(), userMap), row.getMemberRole(), row.getStatus(),
                row.getCompletionStatus(), row.getJoinedAt(), row.getLeftAt());
    }

    private MediaSummary media(FileObject file) {
        return new MediaSummary(file.getId(), file.getOriginalName(), file.getContentType(),
                file.getByteSize() == null ? 0 : file.getByteSize(), file.getWidth(), file.getHeight(),
                file.getStatus(), file.getSortOrder() == null ? 0 : file.getSortOrder(), file.getCreatedAt());
    }

    private List<TimelineEntry> timeline(List<ActivityStatusLog> statusRows, List<AuditLog> auditRows,
                                         List<ReportCase> reportRows, List<ModerationAction> actionRows,
                                         List<AuditLog> governanceAuditRows, Map<Long, SysUser> userMap) {
        List<TimelineEntry> entries = new ArrayList<>();
        statusRows
                .forEach(log -> entries.add(new TimelineEntry(
                        "STATUS", log.getDimensionName(), log.getActionName(), log.getFromStatus(), log.getToStatus(),
                        log.getReason(), displayOperator(log.getOperatorId(), userMap), null, log.getCreatedAt())));
        auditRows
                .forEach(log -> entries.add(new TimelineEntry(
                        "AUDIT", "AUDIT", log.getActionName(), null, null, log.getReason(),
                        displayOperator(log.getOperatorId(), userMap), log.getOperatorRole(), log.getCreatedAt())));
        reportRows.forEach(report -> entries.add(new TimelineEntry(
                "GOVERNANCE", "REPORT", "SUBMIT_REPORT", null, "SUBMITTED", report.getDescription(),
                displayOperator(report.getReporterId(), userMap), "STUDENT", report.getCreatedAt())));
        actionRows.forEach(action -> entries.add(new TimelineEntry(
                "GOVERNANCE", "MODERATION", action.getActionType().name(), null, null, action.getReason(),
                displayOperator(action.getOperatorId(), userMap), null, action.getCreatedAt())));
        governanceAuditRows.forEach(log -> entries.add(new TimelineEntry(
                "GOVERNANCE", "REPORT", log.getActionName(), null, null, log.getReason(),
                displayOperator(log.getOperatorId(), userMap), log.getOperatorRole(), log.getCreatedAt())));
        entries.sort(Comparator.comparing(TimelineEntry::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TimelineEntry::source));
        return List.copyOf(entries);
    }

    private String displayOperator(Long operatorId, Map<Long, SysUser> userMap) {
        if (operatorId == null) return "系统";
        SysUser user = userMap.get(operatorId);
        return user == null || user.getNickname() == null ? "已注销用户" : user.getNickname();
    }

    private List<String> readList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return json.readValue(raw, STRING_LIST);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
