package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping
    public ApiResult<AuthService.UserView> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResult.ok(authService.updateProfile(new AuthService.UpdateProfileCommand(
                request.phone(), request.email()
        )));
    }

    public record UpdateProfileRequest(@Pattern(regexp = "^$|^1\\d{10}$") String phone,
                                       @Email @Size(max = 100) String email) {
    }
}
