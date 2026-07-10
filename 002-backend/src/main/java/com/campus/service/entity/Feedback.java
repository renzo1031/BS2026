package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {
    private Long id;
    private Long requestId;
    private Long userId;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
