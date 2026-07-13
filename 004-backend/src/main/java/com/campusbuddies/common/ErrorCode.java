package com.campusbuddies.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", HttpStatus.BAD_REQUEST, "请求参数不合法"),
    UNAUTHENTICATED("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "请先登录"),
    TOKEN_INVALID("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "登录状态已失效"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "无权执行该操作"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "当前状态不允许该操作"),
    DUPLICATE_APPLICATION("DUPLICATE_APPLICATION", HttpStatus.CONFLICT, "不能重复申请"),
    REVIEW_ALREADY_CLAIMED("REVIEW_ALREADY_CLAIMED", HttpStatus.CONFLICT, "审核任务已被认领"),
    CAPACITY_FULL("CAPACITY_FULL", HttpStatus.CONFLICT, "活动名额已满"),
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION", HttpStatus.CONFLICT, "非法状态流转"),
    FILE_REJECTED("FILE_REJECTED", HttpStatus.BAD_REQUEST, "文件不符合上传规则"),
    RATE_LIMITED("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁"),
    EXTERNAL_SERVICE_UNAVAILABLE("EXTERNAL_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "外部服务暂不可用"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
