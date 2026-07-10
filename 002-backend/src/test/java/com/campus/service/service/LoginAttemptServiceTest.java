package com.campus.service.service;

import com.campus.service.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {
    @Test
    void locksAfterFiveFailuresAndClearsAfterSuccess() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) {
            service.recordFailure("student");
        }

        assertThrows(BusinessException.class, () -> service.checkAllowed("student"));
        service.recordSuccess("student");
        assertDoesNotThrow(() -> service.checkAllowed("student"));
    }
}
