package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service_category")
public class ServiceCategory {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer sortNo;
    private Integer enabled;
}
