package com.campusbuddies.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbuddies.user.UserStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted_at IS NULL LIMIT 1")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE wechat_openid = #{openid} AND deleted_at IS NULL LIMIT 1")
    SysUser findByWechatOpenid(@Param("openid") String openid);

    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    SysUser findByIdForUpdate(@Param("id") long id);

    @Update("UPDATE sys_user SET token_version = token_version + 1, updated_at = UTC_TIMESTAMP(3) WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") long id);

    @Update("""
            UPDATE sys_user
               SET status = #{status}, token_version = token_version + 1, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND role = 'STUDENT' AND deleted_at IS NULL
            """)
    int setStatusAndInvalidateTokens(@Param("id") long id, @Param("status") UserStatus status);
}
