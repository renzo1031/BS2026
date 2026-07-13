package com.campusbuddies.identity;

import com.campusbuddies.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityController {
    public record Submit(@NotNull Long campusId, @NotBlank @Size(max = 32) String identifierType,
                         @NotBlank @Size(max = 64) String identifier, @NotNull Long proofFileId) {}
    public record Decision(int version, boolean approved, @Size(max = 500) String reason) {}
    private final IdentityService service;

    public IdentityController(IdentityService service) { this.service = service; }

    @PostMapping("/api/v1/me/identity-bindings")
    public ApiResponse<IdentityService.View> submit(@Valid @RequestBody Submit input) {
        return ApiResponse.ok(service.submit(input.campusId(), input.identifierType(), input.identifier(), input.proofFileId()));
    }

    @GetMapping("/api/v1/me/identity-bindings/current")
    public ApiResponse<IdentityService.View> current() { return ApiResponse.ok(service.current()); }

    @GetMapping("/api/v1/review/identity-bindings")
    public ApiResponse<List<IdentityService.ReviewView>> pending() { return ApiResponse.ok(service.pending()); }

    @PostMapping("/api/v1/review/identity-bindings/{id}/decision")
    public ApiResponse<IdentityService.View> decide(@PathVariable long id, @Valid @RequestBody Decision input) {
        return ApiResponse.ok(service.decide(id, input.version(), input.approved(), input.reason()));
    }
}
