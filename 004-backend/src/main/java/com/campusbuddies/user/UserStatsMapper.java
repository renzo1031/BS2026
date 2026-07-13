package com.campusbuddies.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserStatsMapper {
    @Select("SELECT COUNT(*) FROM buddy_activity WHERE creator_id = #{userId} AND deleted_at IS NULL")
    long publishedCount(@Param("userId") long userId);

    @Select("""
            SELECT COUNT(*) FROM buddy_member
             WHERE user_id = #{userId} AND member_role = 'PARTICIPANT' AND status = 'ACTIVE'
            """)
    long joinedCount(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM buddy_application WHERE applicant_id = #{userId} AND status = 'PENDING'")
    long pendingApplicationCount(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    long unreadNotificationCount(@Param("userId") long userId);
}
