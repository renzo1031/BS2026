package com.campusbuddies.file;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorage {
    void put(String bucket, String objectKey, InputStream input, long length, String contentType);
    void delete(String bucket, String objectKey);
    String presignedGetUrl(String bucket, String objectKey, Duration ttl);
}
