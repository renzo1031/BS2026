package com.campusbuddies.common;

import java.util.List;

public record PageResult<T>(List<T> records, long total, long page, long size) {
    public static int safeSize(int size) {
        if (size < 1 || size > 50) throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "size 必须在 1 到 50 之间");
        return size;
    }
}
