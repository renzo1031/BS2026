package com.campus.service.controller;

import com.campus.service.common.ApiResult;
import com.campus.service.common.PageResult;
import com.campus.service.common.RequireRole;
import com.campus.service.entity.ServiceRequest;
import com.campus.service.service.RequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @RequireRole("STUDENT")
    public ApiResult<ServiceRequest> create(@Valid @RequestBody CreateRequest request) {
        return ApiResult.ok(requestService.create(new RequestService.CreateRequestCommand(
                request.itemId(), request.title(), request.content(), request.location(), request.repairCategory(),
                request.urgency(), request.certificateType(), request.purpose(), request.language(), request.copies(),
                request.deliveryMethod(), request.venueId(), request.eventName(), request.appointmentStart(),
                request.appointmentEnd(), request.attendeeCount(), request.contactName(), request.contactPhone()
        )));
    }

    @GetMapping("/my")
    @RequireRole("STUDENT")
    public ApiResult<PageResult<ServiceRequest>> my(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(requestService.my(status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<RequestService.RequestDetail> detail(@PathVariable Long id) {
        return ApiResult.ok(requestService.detail(id));
    }

    @PostMapping("/{id}/cancel")
    @RequireRole("STUDENT")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        requestService.cancel(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/feedback")
    @RequireRole("STUDENT")
    public ApiResult<Void> feedback(@PathVariable Long id, @Valid @RequestBody FeedbackRequest request) {
        requestService.feedback(id, new RequestService.FeedbackCommand(request.score(), request.content()));
        return ApiResult.ok();
    }

    @GetMapping(value = "/{id}/certificate", produces = MediaType.TEXT_HTML_VALUE)
    @RequireRole("STUDENT")
    public ResponseEntity<String> certificate(@PathVariable Long id) {
        RequestService.CertificateFile file = requestService.certificate(id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(file.content());
    }

    public record CreateRequest(
            @NotNull Long itemId,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 10000) String content,
            @Size(max = 120) String location,
            @Size(max = 50) String repairCategory,
            @Size(max = 20) String urgency,
            @Size(max = 50) String certificateType,
            @Size(max = 255) String purpose,
            @Size(max = 20) String language,
            @Positive Integer copies,
            @Size(max = 30) String deliveryMethod,
            Long venueId,
            @Size(max = 120) String eventName,
            LocalDateTime appointmentStart,
            LocalDateTime appointmentEnd,
            @Positive Integer attendeeCount,
            @Size(max = 50) String contactName,
            @Size(max = 30) String contactPhone
    ) {
    }

    public record FeedbackRequest(@NotNull @Min(1) @Max(5) Integer score, @Size(max = 500) String content) {
    }
}
