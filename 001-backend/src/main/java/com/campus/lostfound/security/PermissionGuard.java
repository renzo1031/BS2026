package com.campus.lostfound.security;

public final class PermissionGuard {
    private PermissionGuard() {
    }

    public static void requireRole(CurrentUser user, String role) {
        if (user == null || !user.hasRole(role)) {
            throw new SecurityException("无权限执行该操作");
        }
    }

    public static void requireAnyRole(CurrentUser user, String... roles) {
        if (user == null) {
            throw new SecurityException("请先登录");
        }
        for (String role : roles) {
            if (user.hasRole(role)) {
                return;
            }
        }
        throw new SecurityException("无权限执行该操作");
    }

    public static void requireOwnerOrAdmin(CurrentUser user, Long ownerId) {
        if (user == null) {
            throw new SecurityException("请先登录");
        }
        if (user.hasRole("ADMIN") || user.id().equals(ownerId)) {
            return;
        }
        throw new SecurityException("只能操作本人数据");
    }
}
