package com.campusbuddies.file;

public final class StorageUnavailableException extends RuntimeException {
    StorageUnavailableException() { super("对象存储尚未配置"); }
    StorageUnavailableException(Throwable cause) { super("对象存储当前不可用", cause); }
}
