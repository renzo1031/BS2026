package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_role")
public class Role {
    private Long id;
    private String code;
    private String name;
    private String description;
}
