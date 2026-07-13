package com.campusbuddies.user;

import com.campusbuddies.common.ApiResponse;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.file.FileService;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {
    public record ProfileInput(@Size(min = 1, max = 40) String nickname, @Size(max = 300) String bio,
                               @Size(max = 32) String gradeName, @Size(max = 80) String majorName,
                               @Size(max = 500) String interestTagsJson, @Positive Long avatarFileId) {}
    public record View(long id, Long campusId, String nickname, Long avatarFileId,
                       String bio, String gradeName, String majorName,
                       String interestTagsJson, UserRole role, UserStatus status, VerificationStatus verificationStatus) {}
    public record Overview(View user, long publishedCount, long joinedCount, long pendingApplicationCount,
                           long unreadNotificationCount) {}

    private final SysUserMapper users;
    private final UserStatsMapper stats;
    private final FileService files;

    public UserController(SysUserMapper users, UserStatsMapper stats, FileService files) {
        this.users = users;
        this.stats = stats;
        this.files = files;
    }

    @GetMapping
    public ApiResponse<View> current() { return ApiResponse.ok(view(load(SecuritySupport.current().userId()))); }

    @RequestMapping(value = "/profile", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Transactional
    public ApiResponse<View> update(@Valid @RequestBody ProfileInput input) {
        AuthPrincipal principal = SecuritySupport.current();
        if (principal.status() != UserStatus.ACTIVE) throw new BusinessException(ErrorCode.FORBIDDEN);
        SysUser user = loadForUpdate(principal.userId());
        if (input.nickname() != null) user.setNickname(input.nickname().trim());
        if (input.bio() != null) user.setBio(input.bio().trim());
        if (input.gradeName() != null) user.setGradeName(input.gradeName().trim());
        if (input.majorName() != null) user.setMajorName(input.majorName().trim());
        if (input.interestTagsJson() != null) user.setInterestTagsJson(input.interestTagsJson());
        if (input.avatarFileId() != null) {
            SecuritySupport.requireVerifiedStudent(principal);
            files.replaceAvatar(input.avatarFileId(), user.getAvatarFileId(), user.getId(), user.getCampusId());
            user.setAvatarFileId(input.avatarFileId());
        }
        users.updateById(user);
        return ApiResponse.ok(view(user));
    }

    @GetMapping("/overview")
    public ApiResponse<Overview> overview() {
        long userId = SecuritySupport.current().userId();
        SysUser user = load(userId);
        return ApiResponse.ok(new Overview(view(user), stats.publishedCount(userId), stats.joinedCount(userId),
                stats.pendingApplicationCount(userId), stats.unreadNotificationCount(userId)));
    }

    private SysUser load(long id) {
        SysUser user = users.selectById(id);
        if (user == null || user.getStatus() == UserStatus.CLOSED) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        return user;
    }

    private SysUser loadForUpdate(long id) {
        SysUser user = users.findByIdForUpdate(id);
        if (user == null || user.getStatus() == UserStatus.CLOSED) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return user;
    }

    private View view(SysUser user) {
        return new View(user.getId(), user.getCampusId(), user.getNickname(), user.getAvatarFileId(),
                user.getBio(), user.getGradeName(),
                user.getMajorName(), user.getInterestTagsJson(), user.getRole(), user.getStatus(), user.getVerificationStatus());
    }
}
