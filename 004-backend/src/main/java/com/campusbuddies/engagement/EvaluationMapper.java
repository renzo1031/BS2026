package com.campusbuddies.engagement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {
    @Select("SELECT COUNT(*) FROM evaluation WHERE reviewee_id = #{userId}")
    long receivedCount(@Param("userId") long userId);

    @Select("SELECT COALESCE(AVG(rating), 0) FROM evaluation WHERE reviewee_id = #{userId}")
    double averageRating(@Param("userId") long userId);
}
