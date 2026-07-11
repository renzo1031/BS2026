package com.leftbehind.aid.service;

import com.leftbehind.aid.api.ApiModels;
import com.leftbehind.aid.domain.Domain;
import com.leftbehind.aid.mapper.SystemMapper;
import com.leftbehind.aid.security.PlatformPrincipal;
import com.leftbehind.aid.security.SecurityUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final SystemMapper systemMapper;

    public DashboardService(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    public ApiModels.DashboardView summary() {
        PlatformPrincipal principal = SecurityUtils.current();
        Domain.DashboardStats stats = principal.hasRole("VOLUNTEER")
                ? systemMapper.volunteerDashboard(principal.id())
                : systemMapper.dashboardStats(principal.hasRole("SYS_ADMIN") ? null : principal.departmentId());
        return new ApiModels.DashboardView(stats.activeChildren(), stats.pendingRequests(), stats.openRequests(),
                stats.activeVolunteers(), stats.activeAssignments(), stats.completedAssignments());
    }
}
