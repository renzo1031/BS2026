package com.campus.lostfound.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class MapReader {
    private MapReader() {
    }

    public static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    public static String requiredStr(Map<String, Object> map, String key, String label) {
        String value = str(map, key);
        if (value == null || value.isBlank()) {
            throw new BizException(label + "不能为空");
        }
        return value;
    }

    public static Long longValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    public static Long requiredLong(Map<String, Object> map, String key, String label) {
        Long value = longValue(map, key);
        if (value == null) {
            throw new BizException(label + "不能为空");
        }
        return value;
    }

    public static LocalDateTime dateTime(Map<String, Object> map, String key) {
        String value = str(map, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
