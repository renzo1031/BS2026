package com.campus.lostfound.system.notice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {
    @TableId
    private Long id;
    private String noticeType;
    private String title;
    private String content;
    private Long receiverId;
    private String readStatus;
    private String publishStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer popupEnabled;
}
