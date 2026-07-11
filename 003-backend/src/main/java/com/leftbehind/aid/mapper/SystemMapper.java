package com.leftbehind.aid.mapper;

import com.leftbehind.aid.domain.Domain;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface SystemMapper {
    String USER_SELECT = """
            SELECT u.id, u.department_id, u.role_id, u.username, u.password_hash, u.display_name,
                   u.status, u.failed_login_count, u.locked_until, r.code AS role_code,
                   r.data_scope, u.created_at
            FROM sys_user u JOIN sys_role r ON r.id = u.role_id
            """;

    @Select(USER_SELECT + " WHERE u.username = #{username}")
    Domain.UserRow findUserByUsername(String username);

    @Select(USER_SELECT + " WHERE u.id = #{id}")
    Domain.UserRow findUserById(Long id);

    @Select("SELECT id FROM sys_role WHERE code = #{code}")
    Long findRoleIdByCode(String code);

    @Select("""
            SELECT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.code
            """)
    List<String> findPermissionCodes(Long roleId);

    @Insert("""
            INSERT INTO sys_user (department_id, role_id, username, password_hash, display_name, status)
            VALUES (#{departmentId}, #{roleId}, #{username}, #{passwordHash}, #{displayName}, 'ACTIVE')
            """)
    int insertUser(@Param("departmentId") Long departmentId, @Param("roleId") Long roleId,
                   @Param("username") String username, @Param("passwordHash") String passwordHash,
                   @Param("displayName") String displayName);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash} WHERE id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE sys_user
            SET failed_login_count = failed_login_count + 1,
                locked_until = CASE WHEN failed_login_count + 1 >= 5
                    THEN DATE_ADD(NOW(3), INTERVAL 15 MINUTE) ELSE locked_until END
            WHERE id = #{id}
            """)
    int recordLoginFailure(Long id);

    @Update("""
            UPDATE sys_user SET failed_login_count = 0, locked_until = NULL, last_login_at = NOW(3)
            WHERE id = #{id}
            """)
    int recordLoginSuccess(Long id);

    @Update("UPDATE sys_user SET failed_login_count = 0, locked_until = NULL WHERE id = #{id}")
    int resetLoginFailures(Long id);

    @Insert("""
            INSERT INTO auth_session (id, user_id, expires_at)
            VALUES (#{id}, #{userId}, #{expiresAt})
            """)
    int insertSession(@Param("id") String id, @Param("userId") Long userId,
                      @Param("expiresAt") LocalDateTime expiresAt);

    @Select("""
            SELECT COUNT(*)
            FROM auth_session s JOIN sys_user u ON u.id = s.user_id
            WHERE s.id = #{sessionId} AND s.user_id = #{userId}
              AND s.revoked_at IS NULL AND s.expires_at > NOW(3) AND u.status = 'ACTIVE'
            """)
    int isActiveSession(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Update("""
            UPDATE auth_session SET revoked_at = COALESCE(revoked_at, NOW(3))
            WHERE id = #{sessionId} AND user_id = #{userId}
            """)
    int revokeSession(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Update("""
            UPDATE auth_session SET revoked_at = COALESCE(revoked_at, NOW(3))
            WHERE user_id = #{userId} AND revoked_at IS NULL
            """)
    int revokeUserSessions(Long userId);

    @Insert("INSERT INTO volunteer_profile (user_id, certification_status) VALUES (#{userId}, 'UNVERIFIED')")
    int insertVolunteerProfile(Long userId);

    @Select("""
            SELECT vp.user_id, u.username, u.display_name, vp.real_name_encrypted, vp.phone_encrypted,
                   vp.service_region, vp.skills, vp.available_time, vp.introduction,
                   vp.certification_status, vp.rejection_reason, vp.reviewed_by, vp.reviewed_at,
                   vp.created_at, vp.updated_at
            FROM volunteer_profile vp JOIN sys_user u ON u.id = vp.user_id
            WHERE vp.user_id = #{userId}
            """)
    Domain.VolunteerRow findVolunteer(Long userId);

    @Update("""
            UPDATE volunteer_profile
            SET real_name_encrypted = #{realName}, phone_encrypted = #{phone},
                service_region = #{serviceRegion}, skills = #{skills}, available_time = #{availableTime},
                introduction = #{introduction}, certification_status = 'UNVERIFIED', rejection_reason = NULL
            WHERE user_id = #{userId} AND certification_status NOT IN ('PENDING_REVIEW', 'SUSPENDED')
            """)
    int updateVolunteer(@Param("userId") Long userId, @Param("realName") String realName,
                        @Param("phone") String phone, @Param("serviceRegion") String serviceRegion,
                        @Param("skills") String skills, @Param("availableTime") String availableTime,
                        @Param("introduction") String introduction);

    @Update("""
            UPDATE volunteer_profile SET certification_status = 'PENDING_REVIEW', rejection_reason = NULL
            WHERE user_id = #{userId} AND certification_status IN ('UNVERIFIED', 'REJECTED')
              AND real_name_encrypted IS NOT NULL AND phone_encrypted IS NOT NULL
            """)
    int submitVolunteer(Long userId);

    @Update("""
            UPDATE volunteer_profile
            SET certification_status = #{decision}, rejection_reason = #{comment},
                reviewed_by = #{reviewerId}, reviewed_at = NOW(3)
            WHERE user_id = #{userId} AND certification_status = 'PENDING_REVIEW'
            """)
    int reviewVolunteer(@Param("userId") Long userId, @Param("decision") String decision,
                        @Param("comment") String comment, @Param("reviewerId") Long reviewerId);

    @Select("""
            <script>
            SELECT vp.user_id, u.username, u.display_name, vp.real_name_encrypted, vp.phone_encrypted,
                   vp.service_region, vp.skills, vp.available_time, vp.introduction,
                   vp.certification_status, vp.rejection_reason, vp.reviewed_by, vp.reviewed_at,
                   vp.created_at, vp.updated_at
            FROM volunteer_profile vp JOIN sys_user u ON u.id = vp.user_id
            <where>
              <if test="status != null and status != ''">vp.certification_status = #{status}</if>
            </where>
            ORDER BY vp.updated_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.VolunteerRow> listVolunteers(@Param("status") String status, @Param("offset") int offset,
                                             @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM volunteer_profile vp
            <where>
              <if test="status != null and status != ''">vp.certification_status = #{status}</if>
            </where>
            </script>
            """)
    long countVolunteers(String status);

    @Select("SELECT id, code, name, enabled, created_at FROM sys_department ORDER BY name")
    List<Domain.DepartmentRow> listDepartments();

    @Insert("INSERT INTO sys_department (code, name, enabled) VALUES (#{code}, #{name}, 1)")
    int insertDepartment(@Param("code") String code, @Param("name") String name);

    @Select("""
            <script>
            SELECT u.id, u.username, u.display_name, r.code AS role_code, r.name AS role_name,
                   u.department_id, d.name AS department_name, u.status, u.last_login_at, u.created_at
            FROM sys_user u
            JOIN sys_role r ON r.id = u.role_id
            LEFT JOIN sys_department d ON d.id = u.department_id
            <where>
              <if test="keyword != null and keyword != ''">
                (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.display_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="roleCode != null and roleCode != ''">
                AND r.code = #{roleCode}
              </if>
            </where>
            ORDER BY u.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.UserAdminRow> listUsers(@Param("keyword") String keyword, @Param("roleCode") String roleCode,
                                        @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_user u JOIN sys_role r ON r.id = u.role_id
            <where>
              <if test="keyword != null and keyword != ''">
                (u.username LIKE CONCAT('%', #{keyword}, '%') OR u.display_name LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test="roleCode != null and roleCode != ''">AND r.code = #{roleCode}</if>
            </where>
            </script>
            """)
    long countUsers(@Param("keyword") String keyword, @Param("roleCode") String roleCode);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id}")
    int updateUserStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT id, code, name, data_scope FROM sys_role ORDER BY id")
    List<Domain.RoleRow> listRoles();

    @Select("SELECT id, code, name, data_scope FROM sys_role WHERE id = #{id}")
    Domain.RoleRow findRole(Long id);

    @Select("SELECT id, code, name, module FROM sys_permission ORDER BY module, id")
    List<Domain.PermissionRow> listPermissions();

    @Select("SELECT COUNT(*) FROM sys_permission WHERE id = #{id}")
    int permissionExists(Long id);

    @Select("SELECT permission_id FROM sys_role_permission WHERE role_id = #{roleId} ORDER BY permission_id")
    List<Long> findPermissionIds(Long roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deleteRolePermissions(Long roleId);

    @Insert("INSERT INTO sys_role_permission (role_id, permission_id) VALUES (#{roleId}, #{permissionId})")
    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Select("SELECT COUNT(*) FROM sys_role WHERE id = #{roleId}")
    int roleExists(Long roleId);

    @Insert("""
            INSERT INTO audit_log
                (user_id, action, business_type, business_id, before_status, after_status, detail, ip_address)
            VALUES
                (#{userId}, #{action}, #{businessType}, #{businessId}, #{beforeStatus}, #{afterStatus}, #{detail}, #{ipAddress})
            """)
    int insertAudit(@Param("userId") Long userId, @Param("action") String action,
                    @Param("businessType") String businessType, @Param("businessId") String businessId,
                    @Param("beforeStatus") String beforeStatus, @Param("afterStatus") String afterStatus,
                    @Param("detail") String detail, @Param("ipAddress") String ipAddress);

    @Select("""
            <script>
            SELECT a.id, a.user_id, u.username, a.action, a.business_type, a.business_id,
                   a.before_status, a.after_status, a.detail, a.ip_address, a.created_at
            FROM audit_log a LEFT JOIN sys_user u ON u.id = a.user_id
            <where>
              <if test="businessType != null and businessType != ''">a.business_type = #{businessType}</if>
            </where>
            ORDER BY a.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.AuditRow> listAuditLogs(@Param("businessType") String businessType,
                                        @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM audit_log a
            <where>
              <if test="businessType != null and businessType != ''">a.business_type = #{businessType}</if>
            </where>
            </script>
            """)
    long countAuditLogs(String businessType);

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM child_profile WHERE status = 'ACTIVE'
                    AND (#{departmentId} IS NULL OR department_id = #{departmentId})) AS active_children,
                (SELECT COUNT(*) FROM aid_request WHERE status = 'PENDING_REVIEW'
                    AND (#{departmentId} IS NULL OR department_id = #{departmentId})) AS pending_requests,
                (SELECT COUNT(*) FROM aid_request WHERE status IN ('APPROVED','MATCHED','IN_PROGRESS','PENDING_ACCEPTANCE')
                    AND (#{departmentId} IS NULL OR department_id = #{departmentId})) AS open_requests,
                (SELECT COUNT(*) FROM volunteer_profile WHERE certification_status = 'APPROVED') AS active_volunteers,
                (SELECT COUNT(*) FROM service_assignment WHERE status IN ('ASSIGNED','IN_PROGRESS','PENDING_ACCEPTANCE')
                    AND (#{departmentId} IS NULL OR department_id = #{departmentId})) AS active_assignments,
                (SELECT COUNT(*) FROM service_assignment WHERE status = 'COMPLETED'
                    AND (#{departmentId} IS NULL OR department_id = #{departmentId})) AS completed_assignments
            """)
    Domain.DashboardStats dashboardStats(Long departmentId);

    @Select("""
            SELECT
                0 AS active_children,
                0 AS pending_requests,
                (SELECT COUNT(*) FROM aid_request WHERE status = 'APPROVED') AS open_requests,
                (SELECT COUNT(*) FROM volunteer_profile WHERE certification_status = 'APPROVED') AS active_volunteers,
                (SELECT COUNT(*) FROM service_assignment WHERE volunteer_id = #{userId}
                    AND status IN ('ASSIGNED','IN_PROGRESS','PENDING_ACCEPTANCE')) AS active_assignments,
                (SELECT COUNT(*) FROM service_assignment WHERE volunteer_id = #{userId}
                    AND status = 'COMPLETED') AS completed_assignments
            """)
    Domain.DashboardStats volunteerDashboard(Long userId);

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM service_assignment WHERE status = 'COMPLETED') AS completed_services,
                (SELECT COUNT(*) FROM volunteer_profile WHERE certification_status = 'APPROVED') AS approved_volunteers,
                (SELECT COUNT(*) FROM aid_request WHERE status IN ('APPROVED','MATCHED','IN_PROGRESS','PENDING_ACCEPTANCE')) AS active_requests,
                (SELECT COUNT(*) FROM sys_department WHERE enabled = 1) AS service_departments
            """)
    Domain.PublicStats publicStats();
}
