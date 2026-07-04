package com.campus.lostfound.system.user.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.system.user.entity.SysUser;
import com.campus.lostfound.system.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        return Result.ok(authService.register(body));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        return Result.ok(authService.login(body));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(authService.me());
    }

    @PutMapping("/me")
    public Result<SysUser> updateProfile(@RequestBody Map<String, Object> body) {
        return Result.ok(authService.updateProfile(body));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@RequestBody Map<String, Object> body) {
        authService.changePassword(body);
        return Result.ok();
    }
}
