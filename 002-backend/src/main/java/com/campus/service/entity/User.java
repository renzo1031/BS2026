package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private String studentNo;
    private String phone;
    private String email;
    private String college;
    private String major;
    private Long departmentId;
    private String status;
    private Integer tokenVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
