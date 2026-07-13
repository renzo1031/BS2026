package com.campusbuddies.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityTagMapper extends BaseMapper<ActivityTag> {
    @Insert("""
            INSERT IGNORE INTO activity_tag (id, campus_id, name, normalized_name, status)
            VALUES (#{id}, #{campusId}, #{name}, #{normalizedName}, 'ACTIVE')
            """)
    int insertIgnore(@Param("id") long id, @Param("campusId") long campusId,
                     @Param("name") String name, @Param("normalizedName") String normalizedName);

    @Select("SELECT id FROM activity_tag WHERE campus_id = #{campusId} AND normalized_name = #{normalizedName} LIMIT 1")
    Long findId(@Param("campusId") long campusId, @Param("normalizedName") String normalizedName);
}
