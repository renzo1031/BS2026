package com.campusbuddies.file;

import com.campusbuddies.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
public class StorageConfig {
    @Bean
    @Profile("test")
    ObjectStorage inMemoryObjectStorage() {
        return new InMemoryObjectStorage();
    }

    @Bean
    @Profile("!test")
    ObjectStorage objectStorage(AppProperties.Minio properties) {
        if (!StringUtils.hasText(properties.endpoint()) || !StringUtils.hasText(properties.accessKey())
                || !StringUtils.hasText(properties.secretKey())) {
            return new UnavailableObjectStorage();
        }
        return new MinioObjectStorage(properties.endpoint(), properties.accessKey(), properties.secretKey());
    }

    private static final class UnavailableObjectStorage implements ObjectStorage {
        @Override public void put(String bucket, String objectKey, InputStream input, long length, String contentType) {
            throw new StorageUnavailableException();
        }
        @Override public void delete(String bucket, String objectKey) { throw new StorageUnavailableException(); }
        @Override public String presignedGetUrl(String bucket, String objectKey, Duration ttl) {
            throw new StorageUnavailableException();
        }
    }

    static final class InMemoryObjectStorage implements ObjectStorage {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public void put(String bucket, String objectKey, InputStream input, long length, String contentType) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream(Math.toIntExact(length));
                input.transferTo(output);
                objects.put(bucket + "/" + objectKey, output.toByteArray());
            } catch (IOException ex) {
                throw new StorageUnavailableException(ex);
            }
        }

        @Override public void delete(String bucket, String objectKey) { objects.remove(bucket + "/" + objectKey); }

        @Override
        public String presignedGetUrl(String bucket, String objectKey, Duration ttl) {
            if (!objects.containsKey(bucket + "/" + objectKey)) throw new StorageUnavailableException();
            return "memory://" + bucket + "/" + objectKey;
        }
    }
}
