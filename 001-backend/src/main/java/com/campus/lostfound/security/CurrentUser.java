package com.campus.lostfound.security;

import java.util.Set;

public record CurrentUser(Long id, String username, String realName, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public String primaryRole() {
        if (hasRole("ADMIN")) {
            return "ADMIN";
        }
        if (hasRole("STAFF")) {
            return "STAFF";
        }
        return "USER";
    }
}
