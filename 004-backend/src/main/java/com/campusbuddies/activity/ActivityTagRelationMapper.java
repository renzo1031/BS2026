package com.campusbuddies.activity;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityTagRelationMapper {
    @Insert("INSERT IGNORE INTO activity_tag_relation (activity_id, tag_id) VALUES (#{activityId}, #{tagId})")
    int insertIgnore(@Param("activityId") long activityId, @Param("tagId") long tagId);

    @Delete("DELETE FROM activity_tag_relation WHERE activity_id = #{activityId}")
    int deleteByActivity(@Param("activityId") long activityId);

    @Select("""
            SELECT t.name
              FROM activity_tag_relation r
              JOIN activity_tag t ON t.id = r.tag_id
             WHERE r.activity_id = #{activityId} AND t.status = 'ACTIVE'
             ORDER BY t.id
            """)
    List<String> findNames(@Param("activityId") long activityId);
}
