package com.campusbuddies.file;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileAccessMapper {
    @Select("""
            SELECT COUNT(*) FROM sys_user
             WHERE id = #{ownerId} AND avatar_file_id = #{fileId}
               AND status != 'CLOSED' AND deleted_at IS NULL
            """)
    int countActiveAvatar(@Param("fileId") long fileId, @Param("ownerId") long ownerId);

    @Select("""
            SELECT COUNT(*) FROM message msg
              JOIN conversation c ON c.id = msg.conversation_id
              JOIN buddy_member bm ON bm.activity_id = c.activity_id
             WHERE msg.id = #{messageId} AND msg.file_id = #{fileId}
               AND msg.message_type = 'IMAGE' AND msg.status = 'VISIBLE'
               AND bm.user_id = #{userId} AND bm.status = 'ACTIVE'
            """)
    int countActiveConversationMember(@Param("messageId") long messageId,
                                      @Param("fileId") long fileId,
                                      @Param("userId") long userId);
}
