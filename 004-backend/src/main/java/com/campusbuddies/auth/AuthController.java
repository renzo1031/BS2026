package com.campusbuddies.auth;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.security.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public record AdminLogin(@NotBlank String username, @NotBlank @Size(min = 8, max = 100) String password) {}
    public record WechatLogin(@NotBlank String code) {}
    public record Refresh(@NotBlank String refreshToken) {}
    public record Logout(String refreshToken) {}

    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/admin-login")
    public ApiResponse<AuthService.Tokens> adminLogin(@Valid @RequestBody AdminLogin input) {
        return ApiResponse.ok(service.adminLogin(input.username(), input.password()));
    }

    @PostMapping("/wechat-login")
    public ApiResponse<AuthService.Tokens> wechatLogin(@Valid @RequestBody WechatLogin input) {
        return ApiResponse.ok(service.wechatLogin(input.code()));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.Tokens> refresh(@Valid @RequestBody Refresh input) {
        return ApiResponse.ok(service.refresh(input.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) Logout input) {
        service.logout(SecuritySupport.current(), input == null ? null : input.refreshToken());
        return ApiResponse.ok();
    }
}
