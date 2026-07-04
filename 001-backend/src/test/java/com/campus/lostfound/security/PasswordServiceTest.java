package com.campus.lostfound.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    @Test
    void hashesPasswordWithoutStoringPlainText() {
        PasswordService passwordService = new PasswordService();

        String hash = passwordService.hash("password");

        assertFalse(hash.equals("password"));
        assertTrue(passwordService.matches("password", hash));
        assertFalse(passwordService.matches("wrong-password", hash));
    }

    @Test
    void defaultSqlPasswordHashMatchesPassword() {
        PasswordService passwordService = new PasswordService();

        assertTrue(passwordService.matches("password", "$2a$10$tb8TILe98WADMpvBitufyupaypnAYaTCzWFEbWmItbkPhmXldVgHO"));
    }
}
