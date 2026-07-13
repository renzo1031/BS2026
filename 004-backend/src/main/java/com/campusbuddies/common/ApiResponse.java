package com.campusbuddies.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message,
                             @JsonInclude(JsonInclude.Include.ALWAYS) T data,
                             String requestId, Instant timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", "success", data, MDC.get(RequestIdFilter.MDC_KEY), Instant.now());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static ApiResponse<Object> error(ErrorCode error, String message, Object details) {
        return new ApiResponse<>(error.code(), message, details, MDC.get(RequestIdFilter.MDC_KEY), Instant.now());
    }
}
