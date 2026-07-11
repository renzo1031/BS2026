package com.leftbehind.aid.security;

import com.leftbehind.aid.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static PlatformPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof PlatformPrincipal principal)) {
            throw BusinessException.unauthorized("登录状态已失效，请重新登录");
        }
        return principal;
    }
}
