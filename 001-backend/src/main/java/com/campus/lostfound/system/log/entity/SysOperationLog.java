package com.campus.lostfound.system.log.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String targetType;
    private Long targetId;
    private String action;
    private String beforeStatus;
    private String afterStatus;
    private String result;
    private String reason;
    private String requestIp;
    private String userAgent;
    private String requestPath;
    private LocalDateTime createdAt;
}
