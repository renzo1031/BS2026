package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface BuddyMemberMapper extends BaseMapper<BuddyMember> {
    @Select("""
            SELECT COUNT(*) FROM buddy_member
             WHERE activity_id = #{activityId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int isActiveMember(@Param("activityId") long activityId, @Param("userId") long userId);

    @Update("""
            UPDATE buddy_member SET status = 'LEFT', left_at = UTC_TIMESTAMP(3)
             WHERE activity_id = #{activityId} AND user_id = #{userId}
               AND member_role = 'PARTICIPANT' AND status = 'ACTIVE'
            """)
    int markLeft(@Param("activityId") long activityId, @Param("userId") long userId);

    @Select("SELECT * FROM buddy_member WHERE activity_id = #{activityId} AND status = 'ACTIVE' ORDER BY joined_at")
    List<BuddyMember> findActiveMembers(@Param("activityId") long activityId);

    @Update("""
            UPDATE buddy_member SET completion_status = #{status}
             WHERE activity_id = #{activityId} AND user_id = #{userId}
               AND status = 'ACTIVE' AND completion_status = 'PENDING'
            """)
    int setCompletionStatus(@Param("activityId") long activityId, @Param("userId") long userId,
                            @Param("status") CompletionStatus status);

    @Update("""
            UPDATE buddy_member SET completion_status = 'AUTO_CONFIRMED'
             WHERE activity_id = #{activityId} AND status = 'ACTIVE'
               AND completion_status = 'PENDING'
            """)
    int autoConfirmPending(@Param("activityId") long activityId);
}
