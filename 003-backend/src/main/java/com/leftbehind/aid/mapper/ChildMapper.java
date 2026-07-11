package com.leftbehind.aid.mapper;

import com.leftbehind.aid.domain.Domain;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

public interface ChildMapper {
    String SELECT_CHILD = """
            SELECT c.id, c.file_no, c.department_id, d.name AS department_name, c.name_encrypted,
                   c.gender, c.birth_date, c.region, c.school_stage, c.guardian_name_encrypted,
                   c.guardian_phone_encrypted, c.address_encrypted, c.family_summary, c.risk_level,
                   c.status, c.rejection_reason, c.created_by, u.display_name AS creator_name,
                   c.reviewed_by, c.reviewed_at, c.version, c.created_at, c.updated_at
            FROM child_profile c
            JOIN sys_department d ON d.id = c.department_id
            JOIN sys_user u ON u.id = c.created_by
            """;

    @Insert("""
            INSERT INTO child_profile
                (file_no, department_id, name_encrypted, gender, birth_date, region, school_stage,
                 guardian_name_encrypted, guardian_phone_encrypted, address_encrypted,
                 family_summary, risk_level, created_by)
            VALUES
                (#{fileNo}, #{departmentId}, #{nameEncrypted}, #{gender}, #{birthDate}, #{region}, #{schoolStage},
                 #{guardianNameEncrypted}, #{guardianPhoneEncrypted}, #{addressEncrypted},
                 #{familySummary}, #{riskLevel}, #{createdBy})
            """)
    int insert(@Param("fileNo") String fileNo, @Param("departmentId") Long departmentId,
               @Param("nameEncrypted") String nameEncrypted, @Param("gender") String gender,
               @Param("birthDate") LocalDate birthDate, @Param("region") String region,
               @Param("schoolStage") String schoolStage,
               @Param("guardianNameEncrypted") String guardianNameEncrypted,
               @Param("guardianPhoneEncrypted") String guardianPhoneEncrypted,
               @Param("addressEncrypted") String addressEncrypted,
               @Param("familySummary") String familySummary, @Param("riskLevel") String riskLevel,
               @Param("createdBy") Long createdBy);

    @Select(SELECT_CHILD + " WHERE c.file_no = #{fileNo}")
    Domain.ChildRow findByFileNo(String fileNo);

    @Select(SELECT_CHILD + " WHERE c.id = #{id}")
    Domain.ChildRow findById(Long id);

    @Select("""
            <script>
            SELECT c.id, c.file_no, c.department_id, d.name AS department_name, c.name_encrypted,
                   c.gender, c.birth_date, c.region, c.school_stage, c.guardian_name_encrypted,
                   c.guardian_phone_encrypted, c.address_encrypted, c.family_summary, c.risk_level,
                   c.status, c.rejection_reason, c.created_by, u.display_name AS creator_name,
                   c.reviewed_by, c.reviewed_at, c.version, c.created_at, c.updated_at
            FROM child_profile c
            JOIN sys_department d ON d.id = c.department_id
            JOIN sys_user u ON u.id = c.created_by
            <where>
              <if test="departmentId != null">c.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND c.created_by = #{ownerId}</if>
              <if test="status != null and status != ''">AND c.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND (c.file_no LIKE CONCAT('%', #{keyword}, '%') OR c.region LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY c.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.ChildRow> list(@Param("departmentId") Long departmentId, @Param("ownerId") Long ownerId,
                               @Param("status") String status, @Param("keyword") String keyword,
                               @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM child_profile c
            <where>
              <if test="departmentId != null">c.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND c.created_by = #{ownerId}</if>
              <if test="status != null and status != ''">AND c.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND (c.file_no LIKE CONCAT('%', #{keyword}, '%') OR c.region LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            </script>
            """)
    long count(@Param("departmentId") Long departmentId, @Param("ownerId") Long ownerId,
               @Param("status") String status, @Param("keyword") String keyword);

    @Update("""
            UPDATE child_profile
            SET name_encrypted = #{nameEncrypted}, gender = #{gender}, birth_date = #{birthDate},
                region = #{region}, school_stage = #{schoolStage},
                guardian_name_encrypted = #{guardianNameEncrypted},
                guardian_phone_encrypted = #{guardianPhoneEncrypted}, address_encrypted = #{addressEncrypted},
                family_summary = #{familySummary}, risk_level = #{riskLevel},
                status = 'DRAFT', rejection_reason = NULL, reviewed_by = NULL, reviewed_at = NULL,
                version = version + 1
            WHERE id = #{id} AND created_by = #{userId} AND version = #{version}
              AND status IN ('DRAFT', 'REJECTED')
            """)
    int update(@Param("id") Long id, @Param("userId") Long userId, @Param("version") int version,
               @Param("nameEncrypted") String nameEncrypted, @Param("gender") String gender,
               @Param("birthDate") LocalDate birthDate, @Param("region") String region,
               @Param("schoolStage") String schoolStage,
               @Param("guardianNameEncrypted") String guardianNameEncrypted,
               @Param("guardianPhoneEncrypted") String guardianPhoneEncrypted,
               @Param("addressEncrypted") String addressEncrypted,
               @Param("familySummary") String familySummary, @Param("riskLevel") String riskLevel);

    @Update("""
            UPDATE child_profile SET status = 'PENDING_REVIEW', rejection_reason = NULL, version = version + 1
            WHERE id = #{id} AND created_by = #{userId} AND version = #{version}
              AND status IN ('DRAFT', 'REJECTED')
            """)
    int submit(@Param("id") Long id, @Param("userId") Long userId, @Param("version") int version);

    @Update("""
            UPDATE child_profile
            SET status = #{decision}, rejection_reason = #{comment}, reviewed_by = #{reviewerId},
                reviewed_at = NOW(3), version = version + 1
            WHERE id = #{id} AND department_id = #{departmentId} AND version = #{version}
              AND status = 'PENDING_REVIEW' AND created_by != #{reviewerId}
            """)
    int review(@Param("id") Long id, @Param("departmentId") Long departmentId,
               @Param("reviewerId") Long reviewerId, @Param("decision") String decision,
               @Param("comment") String comment, @Param("version") int version);

    @Update("""
            UPDATE child_profile SET status = 'ARCHIVED', version = version + 1
            WHERE id = #{id} AND created_by = #{userId} AND version = #{version} AND status = 'ACTIVE'
            """)
    int archive(@Param("id") Long id, @Param("userId") Long userId, @Param("version") int version);

    @Select("""
            SELECT COUNT(*) FROM aid_request
            WHERE child_id = #{childId} AND status NOT IN ('REJECTED', 'CLOSED', 'CANCELLED')
            """)
    long countOpenAidRequests(Long childId);
}
