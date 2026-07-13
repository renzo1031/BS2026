package com.campusbuddies.file;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ActivityMediaMapper {
    @Insert("INSERT INTO activity_media (activity_id, file_id, sort_order) VALUES (#{activityId}, #{fileId}, #{sortOrder})")
    int insert(@Param("activityId") long activityId, @Param("fileId") long fileId,
               @Param("sortOrder") int sortOrder);

    @Delete("DELETE FROM activity_media WHERE activity_id = #{activityId}")
    int deleteByActivity(@Param("activityId") long activityId);

    @Select("SELECT file_id FROM activity_media WHERE activity_id = #{activityId} ORDER BY sort_order")
    List<Long> findFileIds(@Param("activityId") long activityId);
}
