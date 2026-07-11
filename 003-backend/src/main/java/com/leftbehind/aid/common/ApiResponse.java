package com.leftbehind.aid.common;

import org.slf4j.MDC;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "操作成功", data, currentTraceId());
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, currentTraceId());
    }

    private static String currentTraceId() {
        String value = MDC.get("traceId");
        return value == null ? "" : value;
    }
}
