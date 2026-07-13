package com.campusbuddies.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    @Select("""
            SELECT * FROM message
             WHERE conversation_id = #{conversationId} AND sender_id = #{senderId}
               AND client_message_id = #{clientMessageId}
             LIMIT 1
            """)
    Message findByClientId(@Param("conversationId") long conversationId,
                           @Param("senderId") long senderId,
                           @Param("clientMessageId") String clientMessageId);

    @Select("""
            SELECT * FROM message
             WHERE conversation_id = #{conversationId} AND id > #{afterId} AND status = 'VISIBLE'
             ORDER BY id ASC LIMIT #{limit}
            """)
    List<Message> findAfter(@Param("conversationId") long conversationId,
                            @Param("afterId") long afterId, @Param("limit") int limit);
}
