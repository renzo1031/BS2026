package com.campus.lostfound.lostfound.claim.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.lostfound.claim.entity.LfClaimApplication;
import com.campus.lostfound.lostfound.claim.service.ClaimService;
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
public class ClaimController {
    private final ClaimService claimService;

    @PostMapping("/items/{id}/claims")
    public Result<LfClaimApplication> create(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(claimService.create(id, body));
    }

    @GetMapping("/users/me/claims")
    public Result<PageResult<LfClaimApplication>> mine(@RequestParam Map<String, String> query) {
        return Result.ok(claimService.mine(query));
    }

    @GetMapping("/staff/claims")
    public Result<PageResult<LfClaimApplication>> staffClaims(@RequestParam Map<String, String> query) {
        return Result.ok(claimService.staffPage(query));
    }

    @PostMapping("/staff/claims/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        claimService.approve(id, body);
        return Result.ok();
    }

    @PostMapping("/staff/claims/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        claimService.reject(id, body);
        return Result.ok();
    }

    @PostMapping("/staff/items/{id}/handover")
    public Result<Void> handover(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        claimService.handover(id, body);
        return Result.ok();
    }

    @PutMapping("/staff/items/{id}/custody")
    public Result<Void> updateCustody(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        claimService.updateCustody(id, body);
        return Result.ok();
    }
}
