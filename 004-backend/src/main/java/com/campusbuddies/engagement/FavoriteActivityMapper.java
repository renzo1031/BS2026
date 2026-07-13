package com.campusbuddies.engagement;

import com.campusbuddies.activity.BuddyActivity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FavoriteActivityMapper {
    @Insert("INSERT IGNORE INTO favorite_activity (user_id, activity_id) VALUES (#{userId}, #{activityId})")
    int insertIgnore(@Param("userId") long userId, @Param("activityId") long activityId);

    @Delete("DELETE FROM favorite_activity WHERE user_id = #{userId} AND activity_id = #{activityId}")
    int delete(@Param("userId") long userId, @Param("activityId") long activityId);

    @Select("SELECT COUNT(*) FROM favorite_activity WHERE user_id = #{userId}")
    long count(@Param("userId") long userId);

    @Select("""
            SELECT a.*
              FROM favorite_activity f
              JOIN buddy_activity a ON a.id = f.activity_id AND a.deleted_at IS NULL
             WHERE f.user_id = #{userId}
             ORDER BY f.created_at DESC
             LIMIT #{limit} OFFSET #{offset}
            """)
    List<BuddyActivity> findPage(@Param("userId") long userId, @Param("offset") long offset,
                                 @Param("limit") int limit);
}
