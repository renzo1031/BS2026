package com.campus.lostfound.lostfound.item.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import com.campus.lostfound.lostfound.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping("/items")
    public Result<PageResult<LfItem>> publicItems(@RequestParam Map<String, String> query) {
        return Result.ok(itemService.publicPage(query));
    }

    @GetMapping("/items/{id}")
    public Result<Map<String, Object>> publicDetail(@PathVariable Long id) {
        return Result.ok(itemService.publicDetail(id));
    }

    @PostMapping("/items")
    public Result<LfItem> create(@RequestBody Map<String, Object> body) {
        return Result.ok(itemService.create(body));
    }

    @PutMapping("/items/{id}")
    public Result<LfItem> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(itemService.update(id, body));
    }

    @PostMapping("/items/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        itemService.submit(id);
        return Result.ok();
    }

    @GetMapping("/users/me/items")
    public Result<PageResult<LfItem>> myItems(@RequestParam Map<String, String> query) {
        return Result.ok(itemService.myItems(query));
    }
}
