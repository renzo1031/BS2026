package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_department")
public class Department {
    private Long id;
    private String name;
    private String contactName;
    private String phone;
    private String description;
}
