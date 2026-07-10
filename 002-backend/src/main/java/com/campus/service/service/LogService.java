package com.campus.service.service;

import com.campus.service.common.AuthContext;
import com.campus.service.entity.OperationLog;
import com.campus.service.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogService {
    private final OperationLogMapper logMapper;

    public LogService(OperationLogMapper logMapper) {
        this.logMapper = logMapper;
    }

    public void log(String module, String action, String detail) {
        OperationLog log = new OperationLog();
        AuthContext.AuthUser current = AuthContext.currentOrNull();
        log.setUserId(current == null ? null : current.userId());
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }
}
