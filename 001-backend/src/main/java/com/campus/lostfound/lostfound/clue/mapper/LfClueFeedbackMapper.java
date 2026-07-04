package com.campus.lostfound.lostfound.clue.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.lostfound.clue.entity.LfClueFeedback;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface LfClueFeedbackMapper extends BaseMapper<LfClueFeedback> {
    @Select("""
            <script>
            SELECT
              c.id,
              c.item_id AS itemId,
              i.item_no AS itemNo,
              i.title AS itemTitle,
              c.submitter_id AS submitterId,
              submitter.real_name AS submitterName,
              submitter.phone AS submitterPhone,
              c.clue_content AS clueContent,
              c.clue_image_url AS clueImageUrl,
              c.contact_phone AS contactPhone,
              c.status,
              c.confirmer_id AS confirmerId,
              confirmer.real_name AS confirmerName,
              c.confirm_time AS confirmTime,
              c.confirm_reason AS confirmReason,
              c.created_at AS createdAt,
              c.updated_at AS updatedAt
            FROM lf_clue_feedback c
            LEFT JOIN sys_user submitter ON submitter.id = c.submitter_id
            LEFT JOIN sys_user confirmer ON confirmer.id = c.confirmer_id
            LEFT JOIN lf_item i ON i.id = c.item_id
            WHERE c.deleted = 0
            <if test="status != null and status != ''">AND c.status = #{status}</if>
            ORDER BY c.created_at DESC
            </script>
            """)
    Page<Map<String, Object>> selectAdminPage(Page<Map<String, Object>> page, @Param("status") String status);
}
