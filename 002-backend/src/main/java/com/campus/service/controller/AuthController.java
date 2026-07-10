package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResult<AuthService.UserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(new AuthService.RegisterCommand(
                request.username(), request.password(), request.realName(), request.studentNo(),
                request.phone(), request.email()
        )));
    }

    @PostMapping("/login")
    public ApiResult<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok(authService.login(new AuthService.LoginCommand(request.username(), request.password())));
    }

    @GetMapping("/me")
    public ApiResult<AuthService.UserView> me() {
        return ApiResult.ok(authService.me());
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        authService.logout();
        return ApiResult.ok();
    }

    public record LoginRequest(@NotBlank @Size(max = 50) String username,
                               @NotBlank @Size(max = 100) String password) {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 4, max = 30) @Pattern(regexp = "[A-Za-z0-9_]+") String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 50) String realName,
            @NotBlank @Size(max = 50) String studentNo,
            @Pattern(regexp = "^$|^1\\d{10}$") String phone,
            @Email @Size(max = 100) String email) {
    }
}
