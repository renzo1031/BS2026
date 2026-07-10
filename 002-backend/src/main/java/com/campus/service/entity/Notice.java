package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice")
public class Notice {
    private Long id;
    private Long userId;
    private Long requestId;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdAt;
}
