package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("service_request")
public class ServiceRequest {
    private Long id;
    private String requestNo;
    @Version
    private Integer version;
    private Long itemId;
    private Long applicantId;
    private Long departmentId;
    private Long handlerId;
    private Long venueId;
    private String title;
    private String content;
    private String location;
    private String repairCategory;
    private String urgency;
    private String certificateType;
    private String purpose;
    private String language;
    private Integer copies;
    private String deliveryMethod;
    private String certificateNo;
    private String verificationCode;
    private String eventName;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private Integer attendeeCount;
    private String contactName;
    private String contactPhone;
    private String status;
    private String result;
    private LocalDateTime acceptedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
