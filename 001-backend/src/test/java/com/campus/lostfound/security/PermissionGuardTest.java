package com.campus.lostfound.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionGuardTest {

    @Test
    void adminCanAccessAdminOperationsButUserCannot() {
        CurrentUser admin = new CurrentUser(1L, "admin", "系统管理员", Set.of("ADMIN"));
        CurrentUser user = new CurrentUser(3L, "user", "普通用户", Set.of("USER"));

        assertDoesNotThrow(() -> PermissionGuard.requireRole(admin, "ADMIN"));
        assertThrows(SecurityException.class, () -> PermissionGuard.requireRole(user, "ADMIN"));
    }

    @Test
    void ownerCanEditOwnDraftButNotOthersRecord() {
        CurrentUser owner = new CurrentUser(3L, "user", "普通用户", Set.of("USER"));

        assertDoesNotThrow(() -> PermissionGuard.requireOwnerOrAdmin(owner, 3L));
        assertThrows(SecurityException.class, () -> PermissionGuard.requireOwnerOrAdmin(owner, 4L));
    }
}
