package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service_item")
public class ServiceItem {
    private Long id;
    private Long categoryId;
    private Long departmentId;
    private String code;
    private String name;
    private String type;
    private String description;
    private String requiredMaterials;
    private Integer needVenue;
    private Integer enabled;
}
