package com.campusbuddies.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.campusbuddies.file.StorageUnavailableException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.errorCode();
        return ResponseEntity.status(code.status()).body(ApiResponse.error(code, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(
                ErrorCode.INVALID_ARGUMENT, ErrorCode.INVALID_ARGUMENT.defaultMessage(), fields));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Object>> handleDenied() {
        return ResponseEntity.status(403).body(ApiResponse.error(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), null));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiResponse<Object>> handleDuplicate() {
        return ResponseEntity.status(409).body(ApiResponse.error(ErrorCode.CONFLICT, "数据已存在或操作已提交", null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Object>> handleUploadTooLarge() {
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.FILE_REJECTED, "文件大小超过限制", null));
    }

    @ExceptionHandler(StorageUnavailableException.class)
    ResponseEntity<ApiResponse<Object>> handleStorageUnavailable() {
        return ResponseEntity.status(503).body(ApiResponse.error(
                ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, "对象存储当前不可用", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unhandled request failure", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.error(
                ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null));
    }
}
