package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("venue")
public class Venue {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String status;
    private String description;
}
