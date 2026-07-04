package com.campus.lostfound.lostfound.item.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lf_item_image")
public class LfItemImage extends BaseEntity {
    @TableId
    private Long id;
    private Long itemId;
    private String imageUrl;
    private Integer sortOrder;
}
