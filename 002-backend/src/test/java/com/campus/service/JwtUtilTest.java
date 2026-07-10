package com.campus.service;

import com.campus.service.common.AuthContext;
import com.campus.service.common.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    @Test
    void tokenRoundTripKeepsUserRolesAndVersion() {
        JwtUtil jwtUtil = new JwtUtil("campus-service-platform-secret-key-change-in-production-2026", 1);

        String token = jwtUtil.create(7L, "student", Set.of("STUDENT", "STAFF"), 3);
        AuthContext.AuthUser user = jwtUtil.parse(token);

        assertEquals(7L, user.userId());
        assertEquals("student", user.username());
        assertTrue(user.hasRole("STUDENT"));
        assertTrue(user.hasRole("STAFF"));
        assertFalse(user.hasRole("ADMIN"));
        assertEquals(3, user.tokenVersion());
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil("campus-service-platform-secret-key-change-in-production-2026", -1);
        String token = jwtUtil.create(7L, "student", Set.of("STUDENT"), 0);

        assertThrows(ExpiredJwtException.class, () -> jwtUtil.parse(token));
    }
}
