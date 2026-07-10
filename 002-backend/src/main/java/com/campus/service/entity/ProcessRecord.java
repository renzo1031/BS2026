package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("process_record")
public class ProcessRecord {
    private Long id;
    private Long requestId;
    private Long operatorId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
