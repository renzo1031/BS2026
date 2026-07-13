package com.campusbuddies.governance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ReportCaseMapper extends BaseMapper<ReportCase> {
    @Update("""
            UPDATE report_case
               SET status = 'REVIEWING', assignee_id = #{assigneeId},
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version}
               AND status IN ('SUBMITTED', 'REVIEWING')
               AND (assignee_id IS NULL OR assignee_id = #{assigneeId})
            """)
    int claim(@Param("id") long id, @Param("version") int version, @Param("assigneeId") long assigneeId);

    @Update("""
            UPDATE report_case
               SET status = 'REVIEWING', assignee_id = #{assigneeId}, claim_expires_at = #{expiresAt},
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version}
               AND status IN ('SUBMITTED', 'REVIEWING')
               AND (assignee_id IS NULL OR assignee_id = #{assigneeId}
                    OR claim_expires_at < UTC_TIMESTAMP(3))
            """)
    int claimWithExpiry(@Param("id") long id, @Param("version") int version, @Param("assigneeId") long assigneeId,
                        @Param("expiresAt") java.time.Instant expiresAt);

    @Update("""
            UPDATE report_case
               SET status = 'SUBMITTED', assignee_id = NULL, claim_expires_at = NULL,
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND status = 'REVIEWING' AND claim_expires_at < UTC_TIMESTAMP(3)
            """)
    int releaseExpiredClaim(@Param("id") long id);

    @Update("""
            UPDATE report_case
               SET status = #{status}, resolution = #{resolution},
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND status = 'REVIEWING'
               AND assignee_id = #{assigneeId}
            """)
    int decide(@Param("id") long id, @Param("version") int version,
               @Param("assigneeId") long assigneeId, @Param("status") ReportStatus status,
               @Param("resolution") String resolution);

    @Update("""
            UPDATE report_case
               SET status = 'APPEALED', appeal_reason = #{reason}, appealed_at = UTC_TIMESTAMP(3),
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND status = 'ACTIONED'
            """)
    int appeal(@Param("id") long id, @Param("version") int version, @Param("reason") String reason);

    @Update("""
            UPDATE report_case
               SET status = #{status}, appeal_resolution = #{resolution},
                   version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND status = 'APPEALED'
            """)
    int resolveAppeal(@Param("id") long id, @Param("version") int version,
                      @Param("status") ReportStatus status, @Param("resolution") String resolution);
}
