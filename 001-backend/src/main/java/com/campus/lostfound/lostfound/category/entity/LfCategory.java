package com.campus.lostfound.lostfound.category.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_category")
public class LfCategory extends BaseEntity {
    @TableId
    private Long id;
    private String categoryName;
    private Integer sortOrder;
    private String status;
}
