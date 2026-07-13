package com.campusbuddies.identity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusbuddies.user.SysUser;
import com.campusbuddies.user.VerificationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdentityBindingMapper extends BaseMapper<IdentityBinding> {
    @Select("SELECT * FROM campus_identity_binding WHERE campus_id=#{campusId} AND identifier_type=#{type} " +
            "AND identifier_fingerprint=#{fingerprint} LIMIT 1 FOR UPDATE")
    IdentityBinding findByFingerprintForUpdate(@Param("campusId") long campusId, @Param("type") String type,
                                               @Param("fingerprint") String fingerprint);

    @Select("SELECT * FROM sys_user WHERE id=#{userId} AND deleted_at IS NULL FOR UPDATE")
    SysUser lockUser(@Param("userId") long userId);

    @Select("SELECT * FROM campus_identity_binding WHERE user_id=#{userId} " +
            "AND status IN ('PENDING','APPROVED') LIMIT 1 FOR UPDATE")
    IdentityBinding findCurrentByUserForUpdate(@Param("userId") long userId);

    @Update("""
            UPDATE campus_identity_binding
               SET identifier_ciphertext=#{item.identifierCiphertext},
                   identifier_masked=#{item.identifierMasked}, proof_file_id=#{item.proofFileId},
                   status='PENDING', reviewer_id=NULL, review_reason=NULL,
                   verified_at=NULL, expires_at=NULL, version=version+1,
                   updated_at=UTC_TIMESTAMP(3)
             WHERE id=#{item.id} AND user_id=#{item.userId}
               AND status='REJECTED' AND version=#{expectedVersion}
            """)
    int resubmitRejected(@Param("item") IdentityBinding item, @Param("expectedVersion") int expectedVersion);

    @Update("UPDATE campus_identity_binding SET status=#{status}, reviewer_id=#{reviewerId}, review_reason=#{reason}, " +
            "verified_at=CASE WHEN #{status}='APPROVED' THEN UTC_TIMESTAMP(3) ELSE NULL END, " +
            "version=version+1, updated_at=UTC_TIMESTAMP(3) WHERE id=#{id} AND user_id=#{userId} " +
            "AND status='PENDING' AND version=#{version}")
    int decide(@Param("id") long id, @Param("userId") long userId,
               @Param("version") int version, @Param("status") VerificationStatus status,
               @Param("reviewerId") long reviewerId, @Param("reason") String reason);
}
