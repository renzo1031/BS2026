package com.campus.service.common;

import java.util.Set;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        AuthUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user;
    }

    public static AuthUser currentOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record AuthUser(Long userId, String username, Set<String> roles, Integer tokenVersion) {
        public boolean hasRole(String role) {
            return roles != null && roles.contains(role);
        }
    }
}
