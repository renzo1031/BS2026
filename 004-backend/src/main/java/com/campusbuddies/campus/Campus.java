package com.campusbuddies.campus;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("campus")
public class Campus {
    @TableId public Long id;
    public String name;
    public String code;
    public String status;
    public String identityLabel;
    public Instant createdAt;
    public Instant updatedAt;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getIdentityLabel() { return identityLabel; }
    public String getStatus() { return status; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
