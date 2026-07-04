package com.campus.lostfound.lostfound.item.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.lostfound.item.entity.LfItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface LfItemMapper extends BaseMapper<LfItem> {

    @Select("""
            <script>
            SELECT
              i.id,
              i.item_no AS itemNo,
              i.type,
              i.title,
              i.category_id AS categoryId,
              i.location_id AS locationId,
              i.event_time AS eventTime,
              i.description,
              i.contact_name AS contactName,
              i.contact_phone AS contactPhone,
              i.status,
              i.publisher_id AS publisherId,
              p.real_name AS publisherName,
              p.username AS publisherUsername,
              p.phone AS publisherPhone,
              i.current_claimant_id AS currentClaimantId,
              claimant.real_name AS claimantName,
              claimant.username AS claimantUsername,
              claimant.phone AS claimantPhone,
              i.reviewer_id AS reviewerId,
              reviewer.real_name AS reviewerName,
              i.review_time AS reviewTime,
              i.review_result AS reviewResult,
              i.review_reason AS reviewReason,
              i.custodian_id AS custodianId,
              custodian.real_name AS custodianName,
              i.custody_location AS custodyLocation,
              i.last_operator_id AS lastOperatorId,
              lastop.real_name AS lastOperatorName,
              i.last_operation_summary AS lastOperationSummary,
              i.last_operation_time AS lastOperationTime,
              i.created_at AS createdAt,
              i.updated_at AS updatedAt
            FROM lf_item i
            LEFT JOIN sys_user p ON p.id = i.publisher_id
            LEFT JOIN sys_user claimant ON claimant.id = i.current_claimant_id
            LEFT JOIN sys_user reviewer ON reviewer.id = i.reviewer_id
            LEFT JOIN sys_user custodian ON custodian.id = i.custodian_id
            LEFT JOIN sys_user lastop ON lastop.id = i.last_operator_id
            WHERE i.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (i.title LIKE CONCAT('%', #{keyword}, '%') OR i.item_no LIKE CONCAT('%', #{keyword}, '%') OR i.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="type != null and type != ''">AND i.type = #{type}</if>
            <if test="status != null and status != ''">AND i.status = #{status}</if>
            <if test="categoryId != null and categoryId != ''">AND i.category_id = #{categoryId}</if>
            <if test="locationId != null and locationId != ''">AND i.location_id = #{locationId}</if>
            <if test="publisherKeyword != null and publisherKeyword != ''">
              AND (p.real_name LIKE CONCAT('%', #{publisherKeyword}, '%') OR p.username LIKE CONCAT('%', #{publisherKeyword}, '%') OR p.phone LIKE CONCAT('%', #{publisherKeyword}, '%'))
            </if>
            <if test="claimantKeyword != null and claimantKeyword != ''">
              AND (claimant.real_name LIKE CONCAT('%', #{claimantKeyword}, '%') OR claimant.username LIKE CONCAT('%', #{claimantKeyword}, '%') OR claimant.phone LIKE CONCAT('%', #{claimantKeyword}, '%'))
            </if>
            <if test="reviewerKeyword != null and reviewerKeyword != ''">
              AND (reviewer.real_name LIKE CONCAT('%', #{reviewerKeyword}, '%') OR reviewer.username LIKE CONCAT('%', #{reviewerKeyword}, '%'))
            </if>
            ORDER BY i.last_operation_time DESC, i.updated_at DESC
            </script>
            """)
    Page<Map<String, Object>> selectAdminItemPage(
            Page<Map<String, Object>> page,
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("status") String status,
            @Param("categoryId") String categoryId,
            @Param("locationId") String locationId,
            @Param("publisherKeyword") String publisherKeyword,
            @Param("claimantKeyword") String claimantKeyword,
            @Param("reviewerKeyword") String reviewerKeyword
    );
}
