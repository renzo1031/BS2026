package com.campus.lostfound.lostfound.clue.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_clue_feedback")
public class LfClueFeedback extends BaseEntity {
    @TableId
    private Long id;
    private Long itemId;
    private Long submitterId;
    private String clueContent;
    private String clueImageUrl;
    private String contactPhone;
    private String status;
    private Long confirmerId;
    private LocalDateTime confirmTime;
    private String confirmReason;
}
