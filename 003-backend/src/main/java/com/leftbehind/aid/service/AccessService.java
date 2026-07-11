package com.leftbehind.aid.service;

import com.leftbehind.aid.common.BusinessException;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AccessService {
    public Long queryDepartment() {
        PlatformPrincipal principal = SecurityUtils.current();
        return principal.hasRole("SYS_ADMIN") ? null : principal.departmentId();
    }

    public Long queryOwnerForCaseWorker() {
        PlatformPrincipal principal = SecurityUtils.current();
        return principal.hasRole("CASE_WORKER") ? principal.id() : null;
    }

    public void requireDepartment(Long departmentId) {
        PlatformPrincipal principal = SecurityUtils.current();
        if (!principal.hasRole("SYS_ADMIN") && !Objects.equals(principal.departmentId(), departmentId)) {
            throw BusinessException.forbidden("无权访问其他部门数据");
        }
    }

    public void requireOwner(Long ownerId) {
        if (!Objects.equals(SecurityUtils.current().id(), ownerId)) {
            throw BusinessException.forbidden("只能操作本人负责的数据");
        }
    }

    public Long requireCurrentDepartment() {
        Long departmentId = SecurityUtils.current().departmentId();
        if (departmentId == null) {
            throw BusinessException.forbidden("当前账号未关联业务部门");
        }
        return departmentId;
    }
}
