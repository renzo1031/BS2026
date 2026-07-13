package com.campusbuddies.governance;

import com.campusbuddies.user.SysUser;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBlockMapper {
    @Insert("INSERT IGNORE INTO user_block (blocker_id, blocked_id) VALUES (#{blockerId}, #{blockedId})")
    int insertIgnore(@Param("blockerId") long blockerId, @Param("blockedId") long blockedId);

    @Delete("DELETE FROM user_block WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}")
    int delete(@Param("blockerId") long blockerId, @Param("blockedId") long blockedId);

    @Select("""
            SELECT u.* FROM user_block b JOIN sys_user u ON u.id = b.blocked_id
             WHERE b.blocker_id = #{blockerId} ORDER BY b.created_at DESC
            """)
    List<SysUser> findBlocked(@Param("blockerId") long blockerId);

    @Select("""
            SELECT COUNT(*) FROM user_block
             WHERE (blocker_id = #{firstUserId} AND blocked_id = #{secondUserId})
                OR (blocker_id = #{secondUserId} AND blocked_id = #{firstUserId})
            """)
    int existsEitherDirection(@Param("firstUserId") long firstUserId,
                              @Param("secondUserId") long secondUserId);
}
