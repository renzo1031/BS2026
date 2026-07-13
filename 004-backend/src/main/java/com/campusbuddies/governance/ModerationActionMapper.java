package com.campusbuddies.governance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ModerationActionMapper extends BaseMapper<ModerationAction> {
    @Select("SELECT * FROM moderation_action WHERE report_id = #{reportId} ORDER BY created_at DESC LIMIT 1")
    ModerationAction findLatest(@Param("reportId") long reportId);
}
