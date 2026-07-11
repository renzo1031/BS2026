package com.leftbehind.aid.mapper;

import com.leftbehind.aid.domain.Domain;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

public interface AidMapper {
    String SELECT_AID = """
            SELECT a.id, a.request_no, a.child_id, c.file_no AS child_file_no,
                   c.name_encrypted AS child_name_encrypted, c.birth_date, c.region,
                   a.department_id, d.name AS department_name, a.category, a.title,
                   a.description, a.public_summary, a.priority, a.status, a.rejection_reason,
                   a.created_by, u.display_name AS creator_name, a.reviewed_by, a.reviewed_at,
                   a.version, a.created_at, a.updated_at
            FROM aid_request a
            JOIN child_profile c ON c.id = a.child_id
            JOIN sys_department d ON d.id = a.department_id
            JOIN sys_user u ON u.id = a.created_by
            """;

    String SELECT_APPLICATION = """
            SELECT ap.id, ap.request_id, a.request_no, a.title AS request_title,
                   ap.volunteer_id, u.display_name AS volunteer_name, ap.message, ap.status,
                   ap.decided_by, ap.decided_at, ap.created_at
            FROM aid_application ap
            JOIN aid_request a ON a.id = ap.request_id
            JOIN sys_user u ON u.id = ap.volunteer_id
            """;

    String SELECT_ASSIGNMENT = """
            SELECT s.id, s.request_id, a.request_no, a.title AS request_title, a.category,
                   s.volunteer_id, u.display_name AS volunteer_name, s.department_id,
                   a.child_id, c.file_no AS child_file_no, c.name_encrypted AS child_name_encrypted,
                   c.guardian_name_encrypted, c.guardian_phone_encrypted, c.address_encrypted, c.region,
                   s.status, s.started_at, s.completion_summary, s.submitted_at, s.completed_at,
                   s.version, a.version AS request_version, s.created_at, s.updated_at
            FROM service_assignment s
            JOIN aid_request a ON a.id = s.request_id
            JOIN child_profile c ON c.id = a.child_id
            JOIN sys_user u ON u.id = s.volunteer_id
            """;

    @Insert("""
            INSERT INTO aid_request
                (request_no, child_id, department_id, category, title, description, public_summary,
                 priority, created_by)
            VALUES
                (#{requestNo}, #{childId}, #{departmentId}, #{category}, #{title}, #{description},
                 #{publicSummary}, #{priority}, #{createdBy})
            """)
    int insertAid(@Param("requestNo") String requestNo, @Param("childId") Long childId,
                  @Param("departmentId") Long departmentId, @Param("category") String category,
                  @Param("title") String title, @Param("description") String description,
                  @Param("publicSummary") String publicSummary, @Param("priority") String priority,
                  @Param("createdBy") Long createdBy);

    @Select(SELECT_AID + " WHERE a.request_no = #{requestNo}")
    Domain.AidRow findAidByRequestNo(String requestNo);

    @Select(SELECT_AID + " WHERE a.id = #{id}")
    Domain.AidRow findAidById(Long id);

    @Select(SELECT_AID + " WHERE a.id = #{id} FOR UPDATE")
    Domain.AidRow findAidByIdForUpdate(Long id);

    @Select("""
            <script>
            SELECT a.id, a.request_no, a.child_id, c.file_no AS child_file_no,
                   c.name_encrypted AS child_name_encrypted, c.birth_date, c.region,
                   a.department_id, d.name AS department_name, a.category, a.title,
                   a.description, a.public_summary, a.priority, a.status, a.rejection_reason,
                   a.created_by, u.display_name AS creator_name, a.reviewed_by, a.reviewed_at,
                   a.version, a.created_at, a.updated_at
            FROM aid_request a
            JOIN child_profile c ON c.id = a.child_id
            JOIN sys_department d ON d.id = a.department_id
            JOIN sys_user u ON u.id = a.created_by
            <where>
              <if test="departmentId != null">a.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND a.created_by = #{ownerId}</if>
              <if test="status != null and status != ''">AND a.status = #{status}</if>
              <if test="category != null and category != ''">AND a.category = #{category}</if>
              <if test="keyword != null and keyword != ''">
                AND (a.request_no LIKE CONCAT('%', #{keyword}, '%') OR a.title LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY a.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.AidRow> listAid(@Param("departmentId") Long departmentId, @Param("ownerId") Long ownerId,
                                @Param("status") String status, @Param("category") String category,
                                @Param("keyword") String keyword, @Param("offset") int offset,
                                @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM aid_request a
            <where>
              <if test="departmentId != null">a.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND a.created_by = #{ownerId}</if>
              <if test="status != null and status != ''">AND a.status = #{status}</if>
              <if test="category != null and category != ''">AND a.category = #{category}</if>
              <if test="keyword != null and keyword != ''">
                AND (a.request_no LIKE CONCAT('%', #{keyword}, '%') OR a.title LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            </script>
            """)
    long countAid(@Param("departmentId") Long departmentId, @Param("ownerId") Long ownerId,
                  @Param("status") String status, @Param("category") String category,
                  @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT a.id, a.request_no, a.child_id, c.file_no AS child_file_no,
                   c.name_encrypted AS child_name_encrypted, c.birth_date, c.region,
                   a.department_id, d.name AS department_name, a.category, a.title,
                   a.description, a.public_summary, a.priority, a.status, a.rejection_reason,
                   a.created_by, u.display_name AS creator_name, a.reviewed_by, a.reviewed_at,
                   a.version, a.created_at, a.updated_at
            FROM aid_request a
            JOIN child_profile c ON c.id = a.child_id
            JOIN sys_department d ON d.id = a.department_id
            JOIN sys_user u ON u.id = a.created_by
            WHERE a.status = 'APPROVED'
              <if test="category != null and category != ''">AND a.category = #{category}</if>
              <if test="keyword != null and keyword != ''">
                AND (a.request_no LIKE CONCAT('%', #{keyword}, '%') OR a.title LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY a.priority DESC, a.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.AidRow> listPublicAid(@Param("category") String category, @Param("keyword") String keyword,
                                      @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM aid_request a
            WHERE a.status = 'APPROVED'
              <if test="category != null and category != ''">AND a.category = #{category}</if>
              <if test="keyword != null and keyword != ''">
                AND (a.request_no LIKE CONCAT('%', #{keyword}, '%') OR a.title LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </script>
            """)
    long countPublicAid(@Param("category") String category, @Param("keyword") String keyword);

    @Update("""
            UPDATE aid_request
            SET child_id = #{childId}, department_id = #{departmentId}, category = #{category}, title = #{title},
                description = #{description}, public_summary = #{publicSummary}, priority = #{priority},
                status = 'DRAFT', rejection_reason = NULL, reviewed_by = NULL, reviewed_at = NULL,
                version = version + 1
            WHERE id = #{id} AND created_by = #{userId} AND version = #{version}
              AND status IN ('DRAFT', 'REJECTED')
            """)
    int updateAid(@Param("id") Long id, @Param("userId") Long userId, @Param("version") int version,
                  @Param("childId") Long childId, @Param("departmentId") Long departmentId,
                  @Param("category") String category, @Param("title") String title,
                  @Param("description") String description, @Param("publicSummary") String publicSummary,
                  @Param("priority") String priority);

    @Update("""
            UPDATE aid_request SET status = 'PENDING_REVIEW', rejection_reason = NULL, version = version + 1
            WHERE id = #{id} AND created_by = #{userId} AND version = #{version}
              AND status IN ('DRAFT', 'REJECTED')
            """)
    int submitAid(@Param("id") Long id, @Param("userId") Long userId, @Param("version") int version);

    @Update("""
            UPDATE aid_request
            SET status = #{decision}, rejection_reason = #{comment}, reviewed_by = #{reviewerId},
                reviewed_at = NOW(3), version = version + 1
            WHERE id = #{id} AND department_id = #{departmentId} AND version = #{version}
              AND status = 'PENDING_REVIEW' AND created_by != #{reviewerId}
            """)
    int reviewAid(@Param("id") Long id, @Param("departmentId") Long departmentId,
                  @Param("reviewerId") Long reviewerId, @Param("decision") String decision,
                  @Param("comment") String comment, @Param("version") int version);

    @Insert("""
            INSERT INTO aid_request_review (request_id, reviewer_id, decision, comment)
            VALUES (#{requestId}, #{reviewerId}, #{decision}, #{comment})
            """)
    int insertReview(@Param("requestId") Long requestId, @Param("reviewerId") Long reviewerId,
                     @Param("decision") String decision, @Param("comment") String comment);

    @Update("""
            UPDATE aid_request SET status = #{nextStatus}, version = version + 1
            WHERE id = #{id} AND status = #{expectedStatus} AND version = #{version}
            """)
    int updateAidStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
                        @Param("nextStatus") String nextStatus, @Param("version") int version);

    @Update("""
            UPDATE aid_request SET status = 'CANCELLED', version = version + 1
            WHERE id = #{id} AND status = #{expectedStatus} AND version = #{version}
            """)
    int cancelAid(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
                  @Param("version") int version);

    @Insert("""
            INSERT INTO aid_application (request_id, volunteer_id, message)
            VALUES (#{requestId}, #{volunteerId}, #{message})
            """)
    int insertApplication(@Param("requestId") Long requestId, @Param("volunteerId") Long volunteerId,
                          @Param("message") String message);

    @Select(SELECT_APPLICATION + " WHERE ap.id = #{id}")
    Domain.ApplicationRow findApplication(Long id);

    @Select(SELECT_APPLICATION + " WHERE ap.request_id = #{requestId} ORDER BY ap.created_at DESC")
    List<Domain.ApplicationRow> listApplicationsForRequest(Long requestId);

    @Select(SELECT_APPLICATION + " WHERE ap.volunteer_id = #{volunteerId} ORDER BY ap.created_at DESC")
    List<Domain.ApplicationRow> listApplicationsForVolunteer(Long volunteerId);

    @Update("""
            UPDATE aid_application SET status = 'WITHDRAWN'
            WHERE id = #{id} AND volunteer_id = #{volunteerId} AND status = 'APPLIED'
            """)
    int withdrawApplication(@Param("id") Long id, @Param("volunteerId") Long volunteerId);

    @Update("""
            UPDATE aid_application SET status = 'ACCEPTED', decided_by = #{deciderId}, decided_at = NOW(3)
            WHERE id = #{id} AND request_id = #{requestId} AND status = 'APPLIED'
            """)
    int acceptApplication(@Param("id") Long id, @Param("requestId") Long requestId,
                          @Param("deciderId") Long deciderId);

    @Update("""
            UPDATE aid_application SET status = 'REJECTED', decided_by = #{deciderId}, decided_at = NOW(3)
            WHERE request_id = #{requestId} AND id != #{acceptedId} AND status = 'APPLIED'
            """)
    int rejectOtherApplications(@Param("requestId") Long requestId, @Param("acceptedId") Long acceptedId,
                                @Param("deciderId") Long deciderId);

    @Insert("""
            INSERT INTO service_assignment
                (request_id, application_id, volunteer_id, department_id, status, active_marker)
            VALUES (#{requestId}, #{applicationId}, #{volunteerId}, #{departmentId}, 'ASSIGNED', 1)
            """)
    int insertAssignment(@Param("requestId") Long requestId, @Param("applicationId") Long applicationId,
                         @Param("volunteerId") Long volunteerId, @Param("departmentId") Long departmentId);

    @Select(SELECT_ASSIGNMENT + " WHERE s.application_id = #{applicationId}")
    Domain.AssignmentRow findAssignmentByApplication(Long applicationId);

    @Select(SELECT_ASSIGNMENT + " WHERE s.id = #{id}")
    Domain.AssignmentRow findAssignment(Long id);

    @Select(SELECT_ASSIGNMENT + " WHERE s.request_id = #{requestId} AND s.active_marker = 1")
    Domain.AssignmentRow findActiveAssignmentByRequest(Long requestId);

    @Select("""
            <script>
            SELECT s.id, s.request_id, a.request_no, a.title AS request_title, a.category,
                   s.volunteer_id, u.display_name AS volunteer_name, s.department_id,
                   a.child_id, c.file_no AS child_file_no, c.name_encrypted AS child_name_encrypted,
                   c.guardian_name_encrypted, c.guardian_phone_encrypted, c.address_encrypted, c.region,
                   s.status, s.started_at, s.completion_summary, s.submitted_at, s.completed_at,
                   s.version, a.version AS request_version, s.created_at, s.updated_at
            FROM service_assignment s
            JOIN aid_request a ON a.id = s.request_id
            JOIN child_profile c ON c.id = a.child_id
            JOIN sys_user u ON u.id = s.volunteer_id
            <where>
              <if test="volunteerId != null">s.volunteer_id = #{volunteerId}</if>
              <if test="departmentId != null">AND s.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND a.created_by = #{ownerId}</if>
              <if test="status != null and status != ''">AND s.status = #{status}</if>
            </where>
            ORDER BY s.created_at DESC LIMIT #{offset}, #{size}
            </script>
            """)
    List<Domain.AssignmentRow> listAssignments(@Param("volunteerId") Long volunteerId,
                                                @Param("departmentId") Long departmentId,
                                                @Param("ownerId") Long ownerId,
                                                @Param("status") String status,
                                                @Param("offset") int offset, @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM service_assignment s
            <where>
              <if test="volunteerId != null">s.volunteer_id = #{volunteerId}</if>
              <if test="departmentId != null">AND s.department_id = #{departmentId}</if>
              <if test="ownerId != null">AND EXISTS (SELECT 1 FROM aid_request a WHERE a.id = s.request_id AND a.created_by = #{ownerId})</if>
              <if test="status != null and status != ''">AND s.status = #{status}</if>
            </where>
            </script>
            """)
    long countAssignments(@Param("volunteerId") Long volunteerId, @Param("departmentId") Long departmentId,
                          @Param("ownerId") Long ownerId, @Param("status") String status);

    @Update("""
            UPDATE service_assignment SET status = 'IN_PROGRESS', started_at = NOW(3), version = version + 1
            WHERE id = #{id} AND volunteer_id = #{volunteerId} AND status = 'ASSIGNED' AND version = #{version}
            """)
    int startAssignment(@Param("id") Long id, @Param("volunteerId") Long volunteerId,
                        @Param("version") int version);

    @Insert("""
            INSERT INTO visit_record
                (assignment_id, service_date, duration_minutes, content, result, created_by)
            VALUES (#{assignmentId}, #{serviceDate}, #{durationMinutes}, #{content}, #{result}, #{createdBy})
            """)
    int insertVisit(@Param("assignmentId") Long assignmentId, @Param("serviceDate") LocalDate serviceDate,
                    @Param("durationMinutes") int durationMinutes, @Param("content") String content,
                    @Param("result") String result, @Param("createdBy") Long createdBy);

    @Select("""
            SELECT v.id, v.assignment_id, v.service_date, v.duration_minutes, v.content, v.result,
                   v.created_by, u.display_name AS creator_name, v.created_at
            FROM visit_record v JOIN sys_user u ON u.id = v.created_by
            WHERE v.assignment_id = #{assignmentId}
            ORDER BY v.service_date DESC, v.created_at DESC
            """)
    List<Domain.VisitRow> listVisits(Long assignmentId);

    @Select("SELECT COUNT(*) FROM visit_record WHERE assignment_id = #{assignmentId}")
    long countVisits(Long assignmentId);

    @Update("""
            UPDATE service_assignment
            SET status = 'PENDING_ACCEPTANCE', completion_summary = #{summary}, submitted_at = NOW(3),
                version = version + 1
            WHERE id = #{id} AND volunteer_id = #{volunteerId} AND status = 'IN_PROGRESS' AND version = #{version}
            """)
    int submitCompletion(@Param("id") Long id, @Param("volunteerId") Long volunteerId,
                         @Param("summary") String summary, @Param("version") int version);

    @Update("""
            UPDATE service_assignment
            SET status = 'COMPLETED', completed_at = NOW(3), active_marker = NULL, version = version + 1
            WHERE id = #{id} AND department_id = #{departmentId}
              AND status = 'PENDING_ACCEPTANCE' AND version = #{version}
            """)
    int confirmCompletion(@Param("id") Long id, @Param("departmentId") Long departmentId,
                          @Param("version") int version);

    @Update("""
            UPDATE service_assignment
            SET status = 'TERMINATED', active_marker = NULL, terminated_reason = #{reason}, version = version + 1
            WHERE id = #{id} AND status = 'ASSIGNED' AND version = #{version}
            """)
    int terminateUnstartedAssignment(@Param("id") Long id, @Param("version") int version,
                                     @Param("reason") String reason);

    @Insert("""
            INSERT INTO service_feedback (assignment_id, rating, comment, created_by)
            VALUES (#{assignmentId}, #{rating}, #{comment}, #{createdBy})
            """)
    int insertFeedback(@Param("assignmentId") Long assignmentId, @Param("rating") int rating,
                       @Param("comment") String comment, @Param("createdBy") Long createdBy);
}
