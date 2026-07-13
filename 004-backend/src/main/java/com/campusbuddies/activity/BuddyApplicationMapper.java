package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BuddyApplicationMapper extends BaseMapper<BuddyApplication> {
    @Update("""
            UPDATE buddy_application
               SET status = 'ACCEPTED', decision_reason = NULL, version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND status = 'PENDING'
            """)
    int accept(@Param("id") long id, @Param("version") int version);

    @Update("""
            UPDATE buddy_application
               SET status = 'REJECTED', decision_reason = #{reason}, version = version + 1,
                   updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND version = #{version} AND status = 'PENDING'
            """)
    int reject(@Param("id") long id, @Param("version") int version, @Param("reason") String reason);

    @Update("""
            UPDATE buddy_application
               SET status = 'WITHDRAWN', version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND applicant_id = #{applicantId} AND version = #{version}
               AND status = 'PENDING'
            """)
    int withdrawPending(@Param("id") long id, @Param("applicantId") long applicantId,
                        @Param("version") int version);

    @Update("""
            UPDATE buddy_application
               SET status = 'CANCELLED', version = version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND applicant_id = #{applicantId} AND version = #{version}
               AND status = 'ACCEPTED'
            """)
    int cancelAccepted(@Param("id") long id, @Param("applicantId") long applicantId,
                       @Param("version") int version);
}
