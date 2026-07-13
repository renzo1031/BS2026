package com.campusbuddies.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {
    @Update("""
            UPDATE file_object
               SET status = #{status}, scan_result = #{scanResult}, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND status = 'PENDING_SCAN' AND deleted_at IS NULL
            """)
    int moderate(@Param("id") long id, @Param("status") FileStatus status,
                 @Param("scanResult") String scanResult);

    @Update("""
            UPDATE file_object
               SET business_id = #{businessId}, sort_order = #{sortOrder}, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND owner_id = #{ownerId} AND business_type = #{businessType}
               AND status = 'APPROVED' AND business_id IS NULL AND deleted_at IS NULL
            """)
    int bind(@Param("id") long id, @Param("ownerId") long ownerId,
             @Param("businessType") FileBusinessType businessType,
             @Param("businessId") long businessId, @Param("sortOrder") int sortOrder);

    @Update("""
            UPDATE file_object
               SET business_id = NULL, sort_order = 0, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND owner_id = #{ownerId} AND business_type = 'AVATAR'
               AND business_id = #{ownerId} AND status = 'APPROVED' AND deleted_at IS NULL
            """)
    int unbindAvatar(@Param("id") long id, @Param("ownerId") long ownerId);

    @Update("""
            UPDATE file_object
               SET business_id = #{businessId}, updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND owner_id = #{ownerId} AND campus_id = #{campusId}
               AND business_type = #{businessType} AND business_id IS NULL
               AND status IN ('PENDING_SCAN', 'APPROVED') AND deleted_at IS NULL
            """)
    int bindEvidence(@Param("id") long id, @Param("ownerId") long ownerId,
                     @Param("campusId") long campusId, @Param("businessType") FileBusinessType businessType,
                     @Param("businessId") long businessId);

    @Update("""
            UPDATE file_object SET business_id = NULL, sort_order = 0, updated_at = UTC_TIMESTAMP(3)
             WHERE owner_id = #{ownerId} AND business_type = #{businessType} AND business_id = #{businessId}
               AND deleted_at IS NULL
            """)
    int unbindBusiness(@Param("ownerId") long ownerId, @Param("businessType") FileBusinessType businessType,
                       @Param("businessId") long businessId);

    @Update("""
            UPDATE file_object
               SET status = 'DELETED', deleted_at = UTC_TIMESTAMP(3), updated_at = UTC_TIMESTAMP(3)
             WHERE id = #{id} AND owner_id = #{ownerId} AND business_id IS NULL
               AND status IN ('PENDING_SCAN', 'APPROVED', 'REJECTED') AND deleted_at IS NULL
            """)
    int softDeleteUnbound(@Param("id") long id, @Param("ownerId") long ownerId);
}
