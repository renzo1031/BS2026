package com.campus.lostfound.lostfound.custody.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_custody_handover")
public class LfCustodyHandover extends BaseEntity {
    @TableId
    private Long id;
    private Long itemId;
    private Long claimId;
    private Long custodianId;
    private String custodyLocation;
    private Long receiverId;
    private String receiverName;
    private String receiverPhone;
    private String handoverLocation;
    private LocalDateTime handoverTime;
    private Long handlerId;
    private String status;
    private String remark;
}
