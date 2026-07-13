package com.campusbuddies.file;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class MinioObjectStorage implements ObjectStorage {
    private final MinioClient client;
    private final Set<String> readyBuckets = ConcurrentHashMap.newKeySet();

    MinioObjectStorage(String endpoint, String accessKey, String secretKey) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    @Override
    public void put(String bucket, String objectKey, InputStream input, long length, String contentType) {
        try {
            ensureBucket(bucket);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .stream(input, length, -1).contentType(contentType).build());
        } catch (Exception ex) {
            throw new StorageUnavailableException(ex);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception ex) {
            throw new StorageUnavailableException(ex);
        }
    }

    @Override
    public String presignedGetUrl(String bucket, String objectKey, Duration ttl) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).bucket(bucket).object(objectKey)
                    .expiry(Math.toIntExact(ttl.toSeconds()), TimeUnit.SECONDS).build());
        } catch (Exception ex) {
            throw new StorageUnavailableException(ex);
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        if (readyBuckets.contains(bucket)) return;
        synchronized (readyBuckets) {
            if (!readyBuckets.contains(bucket)) {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                readyBuckets.add(bucket);
            }
        }
    }
}
