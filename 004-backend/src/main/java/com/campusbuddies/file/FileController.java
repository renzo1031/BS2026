package com.campusbuddies.file;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FileController {
    public record ModerateInput(boolean approve, @Size(max = 500) String reason) {}
    public record ActivityMediaInput(@NotNull @Size(max = 6) List<Long> fileIds) {}

    private final FileService service;

    public FileController(FileService service) { this.service = service; }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileService.FileView> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam FileBusinessType businessType,
            @RequestParam(required = false) Long campusId) {
        return ApiResponse.ok(service.upload(file, businessType, campusId));
    }

    @GetMapping("/files")
    public ApiResponse<PageResult<FileService.FileView>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.mine(page, size));
    }

    @GetMapping("/files/{id}")
    public ApiResponse<FileService.FileView> mine(@PathVariable long id) {
        return ApiResponse.ok(service.mine(id));
    }

    @GetMapping("/files/{id}/url")
    public ApiResponse<FileService.FileUrl> accessUrl(@PathVariable long id) {
        return ApiResponse.ok(service.accessUrl(id));
    }

    @DeleteMapping("/files/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/review/files")
    public ApiResponse<PageResult<FileService.FileView>> reviewQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(service.reviewQueue(page, size));
    }

    @PostMapping("/review/files/{id}/decision")
    public ApiResponse<FileService.FileView> moderate(@PathVariable long id,
                                                       @Valid @RequestBody ModerateInput input) {
        return ApiResponse.ok(service.moderate(id, input.approve(), input.reason()));
    }

    @PutMapping("/activities/{activityId}/media")
    public ApiResponse<List<Long>> replaceActivityMedia(@PathVariable long activityId,
                                                         @Valid @RequestBody ActivityMediaInput input) {
        return ApiResponse.ok(service.replaceActivityMedia(activityId, input.fileIds()));
    }
}
