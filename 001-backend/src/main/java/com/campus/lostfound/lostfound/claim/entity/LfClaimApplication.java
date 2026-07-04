package com.campus.lostfound.lostfound.claim.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_claim_application")
public class LfClaimApplication extends BaseEntity {
    @TableId
    private Long id;
    private Long itemId;
    private Long applicantId;
    private String applicantName;
    private String applicantPhone;
    private String proofText;
    private String proofImageUrl;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewReason;
    private String canceledReason;
}
