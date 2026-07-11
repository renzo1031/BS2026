package com.leftbehind.aid.security;

import com.leftbehind.aid.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveDataServiceTest {
    private final SensitiveDataService service = new SensitiveDataService(new AppProperties(
            new AppProperties.Jwt("01234567890123456789012345678901", 120),
            new AppProperties.Pii("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="),
            new AppProperties.Cors("http://localhost:5173")));

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        String first = service.encrypt("测试敏感数据");
        String second = service.encrypt("测试敏感数据");

        assertNotEquals(first, second);
        assertEquals("测试敏感数据", service.decrypt(first));
        assertTrue(first.startsWith("v1:"));
    }

    @Test
    void rejectsTamperedCipherText() {
        String encrypted = service.encrypt("13800138000");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";

        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }
}
