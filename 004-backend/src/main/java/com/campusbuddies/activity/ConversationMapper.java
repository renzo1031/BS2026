package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    @Insert("INSERT IGNORE INTO conversation (id, activity_id, status) VALUES (#{id}, #{activityId}, 'OPEN')")
    int insertIgnore(@Param("id") long id, @Param("activityId") long activityId);

    @Update("UPDATE conversation SET status = #{status}, updated_at = UTC_TIMESTAMP(3) WHERE activity_id = #{activityId}")
    int updateStatus(@Param("activityId") long activityId, @Param("status") String status);

    @Select("""
            SELECT c.* FROM conversation c
              JOIN buddy_member m ON m.activity_id = c.activity_id
             WHERE m.user_id = #{userId} AND m.status = 'ACTIVE'
             ORDER BY c.updated_at DESC
            """)
    List<Conversation> findForUser(@Param("userId") long userId);

    @Select("""
            SELECT m.user_id FROM conversation c
              JOIN buddy_member m ON m.activity_id = c.activity_id
             WHERE c.id = #{conversationId} AND m.status = 'ACTIVE'
            """)
    List<Long> findActiveUserIds(@Param("conversationId") long conversationId);

    @Update("""
            UPDATE conversation SET last_message_id = #{messageId}, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{conversationId}
            """)
    int updateLastMessage(@Param("conversationId") long conversationId, @Param("messageId") long messageId);
}
