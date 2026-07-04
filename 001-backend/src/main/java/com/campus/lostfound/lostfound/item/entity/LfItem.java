package com.campus.lostfound.lostfound.item.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_item")
public class LfItem extends BaseEntity {
    @TableId
    private Long id;
    private String itemNo;
    private String type;
    private String title;
    private Long categoryId;
    private Long locationId;
    private LocalDateTime eventTime;
    private String description;
    private String contactName;
    private String contactPhone;
    private String status;
    private Long publisherId;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewResult;
    private String reviewReason;
    private Long currentClaimantId;
    private Long custodianId;
    private String custodyLocation;
    private LocalDateTime completedTime;
    private String offlineReason;
    private Long lastOperatorId;
    private String lastOperationSummary;
    private LocalDateTime lastOperationTime;
}
