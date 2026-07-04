package com.campus.lostfound.system.catalog.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.lostfound.category.entity.LfCategory;
import com.campus.lostfound.lostfound.category.mapper.LfCategoryMapper;
import com.campus.lostfound.lostfound.location.entity.LfLocation;
import com.campus.lostfound.lostfound.location.mapper.LfLocationMapper;
import com.campus.lostfound.system.notice.entity.SysNotice;
import com.campus.lostfound.system.notice.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {
    private final LfCategoryMapper categoryMapper;
    private final LfLocationMapper locationMapper;
    private final SysNoticeMapper noticeMapper;

    @GetMapping("/categories")
    public Result<List<LfCategory>> categories() {
        return Result.ok(categoryMapper.selectList(new QueryWrapper<LfCategory>().eq("status", "ENABLED").eq("deleted", 0).orderByAsc("sort_order")));
    }

    @GetMapping("/locations")
    public Result<List<LfLocation>> locations() {
        return Result.ok(locationMapper.selectList(new QueryWrapper<LfLocation>().eq("status", "ENABLED").eq("deleted", 0).orderByAsc("sort_order")));
    }

    @GetMapping("/notices")
    public Result<List<SysNotice>> notices() {
        LocalDateTime now = LocalDateTime.now();
        return Result.ok(noticeMapper.selectList(new QueryWrapper<SysNotice>()
                .eq("notice_type", "ANNOUNCEMENT")
                .eq("publish_status", "PUBLISHED")
                .eq("deleted", 0)
                .and(w -> w.isNull("start_time").or().le("start_time", now))
                .and(w -> w.isNull("end_time").or().ge("end_time", now))
                .orderByDesc("popup_enabled")
                .orderByDesc("published_at")
                .last("LIMIT 5")));
    }
}
