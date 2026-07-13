package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BuddyActivityMapper extends BaseMapper<BuddyActivity> {
    @Select("SELECT * FROM buddy_activity WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    BuddyActivity selectForUpdate(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET review_status = 'PENDING', reviewer_id = NULL, claim_expires_at = NULL,
                   review_reason = NULL, version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND creator_id = #{creatorId} AND version = #{version}
               AND deleted_at IS NULL AND lifecycle_status = 'DRAFT'
               AND review_status IN ('NOT_SUBMITTED', 'REJECTED')
            """)
    int submitForReview(@Param("id") long id, @Param("creatorId") long creatorId, @Param("version") int version);

    @Update("""
            UPDATE buddy_activity
               SET reviewer_id = #{reviewerId}, claim_expires_at = #{expiresAt},
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND review_status = 'PENDING'
               AND deleted_at IS NULL
               AND (reviewer_id IS NULL OR reviewer_id = #{reviewerId}
                    OR claim_expires_at < UTC_TIMESTAMP(3))
            """)
    int claim(@Param("id") long id, @Param("version") int version,
              @Param("reviewerId") long reviewerId, @Param("expiresAt") Instant expiresAt);

    @Update("""
            UPDATE buddy_activity
               SET reviewer_id = NULL, claim_expires_at = NULL,
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND review_status = 'PENDING'
               AND claim_expires_at < UTC_TIMESTAMP(3)
               AND deleted_at IS NULL
            """)
    int releaseExpiredClaim(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET review_status = 'APPROVED', lifecycle_status = 'RECRUITING',
                   review_reason = NULL, claim_expires_at = NULL,
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND review_status = 'PENDING'
               AND reviewer_id = #{reviewerId} AND claim_expires_at >= UTC_TIMESTAMP(3)
               AND deleted_at IS NULL
            """)
    int approve(@Param("id") long id, @Param("version") int version, @Param("reviewerId") long reviewerId);

    @Update("""
            UPDATE buddy_activity
               SET review_status = 'REJECTED', lifecycle_status = 'DRAFT',
                   review_reason = #{reason}, claim_expires_at = NULL,
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND review_status = 'PENDING'
               AND reviewer_id = #{reviewerId} AND claim_expires_at >= UTC_TIMESTAMP(3)
               AND deleted_at IS NULL
            """)
    int reject(@Param("id") long id, @Param("version") int version,
               @Param("reviewerId") long reviewerId, @Param("reason") String reason);

    @Update("""
            UPDATE buddy_activity
               SET accepted_count = accepted_count + 1, version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND review_status = 'APPROVED'
               AND lifecycle_status = 'RECRUITING' AND moderation_status = 'NORMAL'
               AND apply_deadline > UTC_TIMESTAMP(3) AND accepted_count < capacity
               AND deleted_at IS NULL
            """)
    int incrementAccepted(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET accepted_count = accepted_count - 1, version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND accepted_count > 0 AND deleted_at IS NULL
            """)
    int decrementAccepted(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET lifecycle_status = 'IN_PROGRESS', version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND creator_id = #{creatorId} AND version = #{version}
               AND review_status = 'APPROVED' AND lifecycle_status = 'RECRUITING'
               AND moderation_status = 'NORMAL' AND accepted_count > 0
               AND start_at <= DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 30 MINUTE)
               AND deleted_at IS NULL
            """)
    int start(@Param("id") long id, @Param("creatorId") long creatorId, @Param("version") int version);

    @Update("""
            UPDATE buddy_activity
               SET lifecycle_status = 'COMPLETION_PENDING', completion_deadline_at = DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 72 HOUR),
                   version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND creator_id = #{creatorId} AND version = #{version}
               AND lifecycle_status = 'IN_PROGRESS' AND deleted_at IS NULL
            """)
    int requestCompletion(@Param("id") long id, @Param("creatorId") long creatorId, @Param("version") int version);

    @Update("""
            UPDATE buddy_activity a
               SET a.lifecycle_status = 'COMPLETED', a.version = a.version + 1,
                   a.updated_at = UTC_TIMESTAMP(3)
             WHERE a.id = #{id} AND a.lifecycle_status = 'COMPLETION_PENDING'
               AND NOT EXISTS (
                   SELECT 1 FROM buddy_member m
                    WHERE m.activity_id = a.id AND m.status = 'ACTIVE'
                      AND m.completion_status NOT IN ('CONFIRMED', 'AUTO_CONFIRMED')
               )
            """)
    int completeIfAllConfirmed(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET lifecycle_status = 'EXPIRED', version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND lifecycle_status = 'RECRUITING'
               AND review_status = 'APPROVED' AND moderation_status = 'NORMAL'
               AND deleted_at IS NULL AND apply_deadline < UTC_TIMESTAMP(3)
            """)
    int expireRecruiting(@Param("id") long id);

    @Update("""
            UPDATE buddy_activity
               SET lifecycle_status = 'CANCELLED',
                   review_status = CASE WHEN review_status = 'PENDING' THEN 'WITHDRAWN' ELSE review_status END,
                   review_reason = #{reason}, version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND creator_id = #{creatorId} AND version = #{version}
               AND lifecycle_status IN ('DRAFT', 'RECRUITING') AND deleted_at IS NULL
            """)
    int cancel(@Param("id") long id, @Param("creatorId") long creatorId,
               @Param("version") int version, @Param("reason") String reason);

    @Update("""
            UPDATE buddy_activity
               SET moderation_status = #{status}, version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND deleted_at IS NULL
            """)
    int setModerationStatus(@Param("id") long id, @Param("status") ActivityModerationStatus status);
}
