package com.campus.lostfound.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId
    private Long id;
    private String username;
    @JsonIgnore
    private String passwordHash;
    private String realName;
    private String phone;
    private String studentNo;
    private String email;
    private String avatarUrl;
    private String status;
    private LocalDateTime lastLoginTime;
}
