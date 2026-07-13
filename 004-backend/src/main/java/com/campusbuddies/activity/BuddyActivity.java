package com.campusbuddies.activity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("buddy_activity")
public class BuddyActivity {
    @TableId
    private Long id;
    private Long campusId;
    private Long creatorId;
    private String sceneName;
    private String title;
    private String description;
    private MeetingMode meetingMode;
    private String publicLocation;
    private String memberLocationDetail;
    private String joinRequirement;
    private String joinQuestionsJson;
    private Instant startAt;
    private Instant endAt;
    private Instant applyDeadline;
    private Integer capacity;
    private Integer acceptedCount;
    private ActivityReviewStatus reviewStatus;
    private ActivityLifecycleStatus lifecycleStatus;
    private ActivityModerationStatus moderationStatus;
    private Long reviewerId;
    private Instant claimExpiresAt;
    private Instant completionDeadlineAt;
    private String reviewReason;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getSceneName() { return sceneName; }
    public void setSceneName(String sceneName) { this.sceneName = sceneName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MeetingMode getMeetingMode() { return meetingMode; }
    public void setMeetingMode(MeetingMode meetingMode) { this.meetingMode = meetingMode; }
    public String getPublicLocation() { return publicLocation; }
    public void setPublicLocation(String publicLocation) { this.publicLocation = publicLocation; }
    public String getMemberLocationDetail() { return memberLocationDetail; }
    public void setMemberLocationDetail(String memberLocationDetail) { this.memberLocationDetail = memberLocationDetail; }
    public String getJoinRequirement() { return joinRequirement; }
    public void setJoinRequirement(String joinRequirement) { this.joinRequirement = joinRequirement; }
    public String getJoinQuestionsJson() { return joinQuestionsJson; }
    public void setJoinQuestionsJson(String joinQuestionsJson) { this.joinQuestionsJson = joinQuestionsJson; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public Instant getApplyDeadline() { return applyDeadline; }
    public void setApplyDeadline(Instant applyDeadline) { this.applyDeadline = applyDeadline; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getAcceptedCount() { return acceptedCount; }
    public void setAcceptedCount(Integer acceptedCount) { this.acceptedCount = acceptedCount; }
    public ActivityReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ActivityReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public ActivityLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    public void setLifecycleStatus(ActivityLifecycleStatus lifecycleStatus) { this.lifecycleStatus = lifecycleStatus; }
    public ActivityModerationStatus getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(ActivityModerationStatus moderationStatus) { this.moderationStatus = moderationStatus; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public Instant getClaimExpiresAt() { return claimExpiresAt; }
    public void setClaimExpiresAt(Instant claimExpiresAt) { this.claimExpiresAt = claimExpiresAt; }
    public Instant getCompletionDeadlineAt() { return completionDeadlineAt; }
    public void setCompletionDeadlineAt(Instant completionDeadlineAt) { this.completionDeadlineAt = completionDeadlineAt; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
