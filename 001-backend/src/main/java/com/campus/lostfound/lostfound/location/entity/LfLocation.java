package com.campus.lostfound.lostfound.location.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_location")
public class LfLocation extends BaseEntity {
    @TableId
    private Long id;
    private String locationName;
    private String areaName;
    private Integer sortOrder;
    private String status;
}
