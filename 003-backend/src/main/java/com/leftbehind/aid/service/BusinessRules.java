package com.leftbehind.aid.service;

import com.leftbehind.aid.common.BusinessException;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class BusinessRules {
    private static final Pattern SENSITIVE_PUBLIC_TEXT =
            Pattern.compile("(?<!\\d)1\\d{10}(?!\\d)|(?<!\\d)\\d{17}[0-9Xx](?!\\d)");

    private BusinessRules() {
    }

    public static void requireState(String actual, String... allowed) {
        if (Arrays.stream(allowed).noneMatch(actual::equals)) {
            throw BusinessException.conflict("当前状态不允许执行该操作");
        }
    }

    public static void requireUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw BusinessException.conflict("数据已发生变化，请刷新后重试");
        }
    }

    public static void requirePublicSummarySafe(String summary) {
        if (SENSITIVE_PUBLIC_TEXT.matcher(summary).find()) {
            throw BusinessException.badRequest("公开摘要不能包含手机号或身份证号");
        }
    }

    public static String rejectionReason(String decision, String comment) {
        if (!"REJECTED".equals(decision)) {
            return null;
        }
        if (comment == null || comment.isBlank()) {
            throw BusinessException.badRequest("驳回时必须填写原因");
        }
        return comment.trim();
    }
}
