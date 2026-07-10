package com.campus.service.common;

import java.util.List;

public record PageResult<T>(long total, long page, long size, List<T> records) {
}
