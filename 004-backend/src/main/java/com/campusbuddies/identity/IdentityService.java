package com.campusbuddies.identity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusbuddies.common.BusinessException;
import com.campusbuddies.common.ErrorCode;
import com.campusbuddies.security.AuthPrincipal;
import com.campusbuddies.security.SecuritySupport;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.SysUserMapper;
import com.campusbuddies.user.UserRole;
import com.campusbuddies.user.UserStatus;
import com.campusbuddies.user.VerificationStatus;
import com.campusbuddies.file.FileBusinessType;
import com.campusbuddies.file.FileService;
import com.campusbuddies.notification.NotificationService;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    public record View(long id, long userId, long campusId, String identifierType, String identifierMasked,
                       Long proofFileId, VerificationStatus status, String reviewReason, int version) {}
    public record ReviewView(View binding, String identifierPlaintext) {}

    private final IdentityBindingMapper bindings;
    private final SysUserMapper users;
    private final IdentityProtector protector;
    private final FileService files;
    private final NotificationService notifications;

    public IdentityService(IdentityBindingMapper bindings, SysUserMapper users,
                           IdentityProtector protector, FileService files,
                           NotificationService notifications) {
        this.bindings = bindings;
        this.users = users;
        this.protector = protector;
        this.files = files;
        this.notifications = notifications;
    }

    @Transactional
    public View submit(long campusId, String identifierType, String identifier, long proofFileId) {
        AuthPrincipal principal = SecuritySupport.current();
        if (principal.role() != UserRole.STUDENT) throw new BusinessException(ErrorCode.FORBIDDEN);
        files.requireOwnedEvidence(proofFileId, principal.userId(), campusId, FileBusinessType.IDENTITY_PROOF);
        String type = identifierType.trim().toUpperCase(java.util.Locale.ROOT);
        String normalized;
        try {
            normalized = protector.normalize(identifier);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, ex.getMessage());
        }
        String fingerprint = protector.fingerprint(campusId, type, normalized);

        // 同一用户的提交和审核都先锁用户行，避免审核结论覆盖刚提交的新校园。
        SysUser user = bindings.lockUser(principal.userId());
        if (user == null || user.getRole() != UserRole.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (user.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账号已经完成校园认证");
        }
        if (bindings.findCurrentByUserForUpdate(principal.userId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "当前账号已有认证申请或已完成认证");
        }

        IdentityBinding binding = bindings.findByFingerprintForUpdate(campusId, type, fingerprint);
        if (binding == null) {
            binding = new IdentityBinding();
            binding.userId = principal.userId();
            binding.campusId = campusId;
            binding.identifierType = type;
            binding.identifierFingerprint = fingerprint;
            binding.identifierCiphertext = protector.encrypt(campusId, type, normalized);
            binding.identifierMasked = protector.mask(normalized);
            binding.proofFileId = proofFileId;
            binding.status = VerificationStatus.PENDING;
            binding.version = 0;
            try {
                bindings.insert(binding);
            } catch (DuplicateKeyException ex) {
                throw new BusinessException(ErrorCode.CONFLICT, "该校园身份已绑定或正在审核");
            }
        } else {
            if (!binding.userId.equals(principal.userId()) || binding.status != VerificationStatus.REJECTED) {
                throw new BusinessException(ErrorCode.CONFLICT, "该校园身份已绑定或正在审核");
            }
            int expectedVersion = binding.version == null ? 0 : binding.version;
            binding.identifierCiphertext = protector.encrypt(campusId, type, normalized);
            binding.identifierMasked = protector.mask(normalized);
            binding.proofFileId = proofFileId;
            if (bindings.resubmitRejected(binding, expectedVersion) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "认证状态已变化，请刷新后重试");
            }
            binding.status = VerificationStatus.PENDING;
            binding.reviewerId = null;
            binding.reviewReason = null;
            binding.verifiedAt = null;
            binding.expiresAt = null;
            binding.version = expectedVersion + 1;
        }
        files.bindEvidence(proofFileId, principal.userId(), campusId,
                FileBusinessType.IDENTITY_PROOF, binding.id);
        user.setCampusId(campusId);
        user.setVerificationStatus(VerificationStatus.PENDING);
        if (users.updateById(user) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号状态已变化，请重新登录后重试");
        }
        return view(binding);
    }

    public View current() {
        IdentityBinding binding = bindings.selectOne(new LambdaQueryWrapper<IdentityBinding>()
                .eq(IdentityBinding::getUserId, SecuritySupport.current().userId())
                .orderByDesc(IdentityBinding::getCreatedAt).last("LIMIT 1"));
        return binding == null ? null : view(binding);
    }

    public List<ReviewView> pending() {
        AuthPrincipal reviewer = SecuritySupport.current();
        SecuritySupport.requireReviewer(reviewer);
        LambdaQueryWrapper<IdentityBinding> query = new LambdaQueryWrapper<IdentityBinding>()
                .eq(IdentityBinding::getStatus, VerificationStatus.PENDING)
                .orderByAsc(IdentityBinding::getCreatedAt);
        if (!reviewer.isPlatformAdmin()) query.eq(IdentityBinding::getCampusId, reviewer.campusId());
        return bindings.selectList(query).stream().map(this::reviewView).toList();
    }

    @Transactional
    public View decide(long id, int version, boolean approve, String reason) {
        AuthPrincipal reviewer = SecuritySupport.current();
        SecuritySupport.requireReviewer(reviewer);
        IdentityBinding candidate = bindings.selectById(id);
        if (candidate == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        SysUser user = bindings.lockUser(candidate.userId);
        if (user == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        IdentityBinding binding = bindings.findCurrentByUserForUpdate(candidate.userId);
        int currentVersion = binding == null || binding.version == null ? -1 : binding.version;
        if (binding == null || !binding.id.equals(id) || binding.status != VerificationStatus.PENDING
                || currentVersion != version
                || user.getVerificationStatus() != VerificationStatus.PENDING
                || !Objects.equals(user.getCampusId(), binding.campusId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "认证状态已变化，请刷新后重试");
        }
        SecuritySupport.requireCampus(reviewer, binding.campusId);
        if (approve) {
            files.requireApprovedEvidence(binding.proofFileId, binding.userId, binding.campusId,
                    FileBusinessType.IDENTITY_PROOF, binding.id);
        }
        VerificationStatus target = approve ? VerificationStatus.APPROVED : VerificationStatus.REJECTED;
        if (bindings.decide(id, binding.userId, version, target, reviewer.userId(), reason) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "认证状态已变化，请刷新后重试");
        }
        user.setCampusId(binding.campusId);
        user.setVerificationStatus(target);
        int currentTokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentTokenVersion + 1);
        if (users.updateById(user) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "账号状态已变化，请刷新后重试");
        }
        notifications.create(user.getId(), approve ? "IDENTITY_APPROVED" : "IDENTITY_REJECTED",
                approve ? "校园身份认证已通过" : "校园身份认证未通过",
                approve ? "现在可以发布和申请校园搭子活动" : "请根据审核理由重新提交认证材料",
                "IDENTITY_BINDING", id);
        binding.status = target;
        binding.reviewerId = reviewer.userId();
        binding.reviewReason = reason;
        binding.version = version + 1;
        return view(binding);
    }

    private ReviewView reviewView(IdentityBinding binding) {
        return new ReviewView(view(binding), protector.decrypt(binding.campusId, binding.identifierType, binding.identifierCiphertext));
    }

    private View view(IdentityBinding b) {
        return new View(b.id, b.userId, b.campusId, b.identifierType, b.identifierMasked, b.proofFileId,
                b.status, b.reviewReason, b.version == null ? 0 : b.version);
    }
}
