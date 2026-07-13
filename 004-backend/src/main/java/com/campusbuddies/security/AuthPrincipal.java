package com.campusbuddies.security;

import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;

public record AuthPrincipal(
        long userId,
        Long campusId,
        UserRole role,
        UserStatus status,
        VerificationStatus verificationStatus,
        int tokenVersion,
        String tokenId,
        long tokenExpiresAtEpochSecond) {

    public boolean isPlatformAdmin() { return status == UserStatus.ACTIVE && role == UserRole.PLATFORM_ADMIN; }
    public boolean isReviewer() {
        return status == UserStatus.ACTIVE && (role == UserRole.CAMPUS_REVIEWER || role == UserRole.PLATFORM_ADMIN);
    }
    public boolean isVerifiedStudent() {
        return status == UserStatus.ACTIVE && role == UserRole.STUDENT
                && verificationStatus == VerificationStatus.APPROVED;
    }
}
