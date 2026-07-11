package com.leftbehind.aid.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedPasswordTest {
    @Test
    void documentedDemoPasswordMatchesSeedHash() {
        String hash = "$2b$12$Lo/HtBcyESgPVbYzXsYsEu29.Q8iUINGdaUMreqAtVA1/emMROKoG";
        assertTrue(new BCryptPasswordEncoder().matches("123456", hash));
    }
}
