package com.campusbuddies.security;

import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecuritySupport {
    private SecuritySupport() {}

    public static AuthPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return principal;
    }

    public static void requireVerifiedStudent(AuthPrincipal principal) {
        if (!principal.isVerifiedStudent()) throw new BusinessException(ErrorCode.FORBIDDEN, "完成校园身份认证后才能操作");
    }

    public static void requireReviewer(AuthPrincipal principal) {
        if (!principal.isReviewer()) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    public static void requirePlatformAdmin(AuthPrincipal principal) {
        if (!principal.isPlatformAdmin()) throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    public static void requireCampus(AuthPrincipal principal, Long campusId) {
        if (!principal.isPlatformAdmin() && (principal.campusId() == null || !principal.campusId().equals(campusId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "禁止跨校园操作");
        }
    }
}
