package com.campusbuddies.campus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbuddies.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campuses")
public class CampusController {
    public record View(long id, String name, String code, String identityLabel) {}
    private final CampusMapper campuses;

    public CampusController(CampusMapper campuses) { this.campuses = campuses; }

    @GetMapping
    public ApiResponse<List<View>> list() {
        List<View> result = campuses.selectList(new LambdaQueryWrapper<Campus>().eq(Campus::getStatus, "ACTIVE")
                        .orderByAsc(Campus::getName)).stream()
                .map(c -> new View(c.id, c.name, c.code, c.identityLabel)).toList();
        return ApiResponse.ok(result);
    }
}
