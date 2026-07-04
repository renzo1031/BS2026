package com.campus.lostfound.system.menu.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.lostfound.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    @TableId
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuCode;
    private String path;
    private String component;
    private String permissionCode;
    private Integer sortOrder;
    private Integer visible;
}
