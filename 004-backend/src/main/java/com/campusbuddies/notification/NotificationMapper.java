package com.campusbuddies.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId} AND is_read = 0")
    int markRead(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") long userId);
}
