package com.leftbehind.aid.service;

import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.security.PlatformPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessServiceTest {
    private final AccessService accessService = new AccessService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsCrossDepartmentAccess() {
        authenticate(new PlatformPrincipal(3L, 1L, "worker", "个案人员", "CASE_WORKER",
                "DEPARTMENT", Set.of("child:read"), "session"));

        assertDoesNotThrow(() -> accessService.requireDepartment(1L));
        assertThrows(BusinessException.class, () -> accessService.requireDepartment(2L));
    }

    @Test
    void allowsGlobalAdministratorToReadAnyDepartment() {
        authenticate(new PlatformPrincipal(1L, null, "admin", "管理员", "SYS_ADMIN",
                "GLOBAL", Set.of("child:read"), "session"));

        assertDoesNotThrow(() -> accessService.requireDepartment(99L));
    }

    private void authenticate(PlatformPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, Set.of()));
    }
}
