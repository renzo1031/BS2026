package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.entity.Notice;
import com.campus.service.service.NoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/my")
    public ApiResult<List<Notice>> my() {
        return ApiResult.ok(noticeService.my());
    }

    @PostMapping("/{id}/read")
    public ApiResult<Void> read(@PathVariable Long id) {
        noticeService.read(id);
        return ApiResult.ok();
    }
}
