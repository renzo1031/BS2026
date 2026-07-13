package com.campusbuddies.identity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campusbuddies.user.VerificationStatus;
import java.time.Instant;

@TableName("campus_identity_binding")
public class IdentityBinding {
    @TableId public Long id;
    public Long userId;
    public Long campusId;
    public String identifierType;
    public String identifierFingerprint;
    public String identifierCiphertext;
    public String identifierMasked;
    public Long proofFileId;
    public VerificationStatus status;
    public Long reviewerId;
    public String reviewReason;
    public Instant verifiedAt;
    public Instant expiresAt;
    public Integer version;
    public Instant createdAt;
    public Instant updatedAt;

    public Long getUserId() { return userId; }
    public Long getCampusId() { return campusId; }
    public VerificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
