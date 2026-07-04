package com.campus.lostfound.security;

public final class LoginContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new SecurityException("请先登录");
        }
        return user;
    }

    public static Long userIdOrNull() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.id();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
