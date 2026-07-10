package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_identity")
public class StudentIdentity {
    private Long id;
    private String studentNo;
    private String realName;
    private String college;
    private String major;
    private String status;
    private Long boundUserId;
}
