package com.leftbehind.aid.security;

import java.util.Set;

public record PlatformPrincipal(
        Long id, Long departmentId, String username, String displayName, String roleCode,
        String dataScope, Set<String> permissions, String sessionId
) {
    public boolean hasRole(String role) {
        return roleCode.equals(role);
    }
}
