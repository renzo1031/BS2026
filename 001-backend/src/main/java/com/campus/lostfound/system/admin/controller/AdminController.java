package com.campus.lostfound.system.admin.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.lostfound.category.entity.LfCategory;
import com.campus.lostfound.lostfound.claim.entity.LfClaimApplication;
import com.campus.lostfound.lostfound.claim.service.ClaimService;
import com.campus.lostfound.lostfound.clue.service.ClueService;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import com.campus.lostfound.lostfound.item.service.ItemService;
import com.campus.lostfound.lostfound.location.entity.LfLocation;
import com.campus.lostfound.system.admin.service.AdminService;
import com.campus.lostfound.system.log.entity.SysOperationLog;
import com.campus.lostfound.system.notice.entity.SysNotice;
import com.campus.lostfound.system.user.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ItemService itemService;
    private final ClaimService claimService;
    private final ClueService clueService;
    private final AdminService adminService;

    @GetMapping("/items")
    public Result<PageResult<Map<String, Object>>> items(@RequestParam Map<String, String> query) {
        return Result.ok(itemService.adminPage(query));
    }

    @GetMapping("/items/{id}")
    public Result<Map<String, Object>> itemDetail(@PathVariable Long id) {
        return Result.ok(itemService.adminDetail(id));
    }

    @PostMapping("/items/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        itemService.review(id, body);
        return Result.ok();
    }

    @PostMapping("/items/{id}/offline")
    public Result<Void> offline(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        itemService.offline(id, body);
        return Result.ok();
    }

    @PostMapping("/items/{id}/archive")
    public Result<Void> archive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        itemService.archive(id, body);
        return Result.ok();
    }

    @GetMapping("/clues")
    public Result<PageResult<Map<String, Object>>> clues(@RequestParam Map<String, String> query) {
        return Result.ok(clueService.adminPage(query));
    }

    @GetMapping("/claims")
    public Result<PageResult<LfClaimApplication>> claims(@RequestParam Map<String, String> query) {
        return Result.ok(claimService.staffPage(query));
    }

    @GetMapping("/users")
    public Result<PageResult<SysUser>> users(@RequestParam Map<String, String> query) {
        return Result.ok(adminService.users(query));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminService.updateUserStatus(id, body);
        return Result.ok();
    }

    @PostMapping("/users/{id}/password/reset")
    public Result<Void> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminService.resetUserPassword(id, body);
        return Result.ok();
    }

    @GetMapping("/categories")
    public Result<PageResult<LfCategory>> categories(@RequestParam Map<String, String> query) {
        return Result.ok(adminService.categories(query));
    }

    @PostMapping("/categories")
    public Result<LfCategory> createCategory(@RequestBody Map<String, Object> body) {
        return Result.ok(adminService.saveCategory(body));
    }

    @PutMapping("/categories/{id}")
    public Result<LfCategory> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return Result.ok(adminService.saveCategory(body));
    }

    @GetMapping("/locations")
    public Result<PageResult<LfLocation>> locations(@RequestParam Map<String, String> query) {
        return Result.ok(adminService.locations(query));
    }

    @PostMapping("/locations")
    public Result<LfLocation> createLocation(@RequestBody Map<String, Object> body) {
        return Result.ok(adminService.saveLocation(body));
    }

    @PutMapping("/locations/{id}")
    public Result<LfLocation> updateLocation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return Result.ok(adminService.saveLocation(body));
    }

    @GetMapping("/notices")
    public Result<PageResult<SysNotice>> notices(@RequestParam Map<String, String> query) {
        return Result.ok(adminService.notices(query));
    }

    @PostMapping("/notices")
    public Result<SysNotice> createNotice(@RequestBody Map<String, Object> body) {
        return Result.ok(adminService.saveNotice(body));
    }

    @PutMapping("/notices/{id}")
    public Result<SysNotice> updateNotice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        return Result.ok(adminService.saveNotice(body));
    }

    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        adminService.deleteNotice(id);
        return Result.ok();
    }

    @GetMapping("/logs")
    public Result<PageResult<SysOperationLog>> logs(@RequestParam Map<String, String> query) {
        return Result.ok(adminService.logs(query));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.ok(adminService.statistics());
    }
}
