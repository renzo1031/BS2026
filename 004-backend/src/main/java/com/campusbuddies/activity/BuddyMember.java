package com.campusbuddies.activity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("buddy_member")
public class BuddyMember {
    @TableId
    private Long id;
    private Long activityId;
    private Long userId;
    private MemberRole memberRole;
    private MemberStatus status;
    private CompletionStatus completionStatus;
    private Instant joinedAt;
    private Instant leftAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public MemberRole getMemberRole() { return memberRole; }
    public void setMemberRole(MemberRole memberRole) { this.memberRole = memberRole; }
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public CompletionStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(CompletionStatus completionStatus) { this.completionStatus = completionStatus; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
}
