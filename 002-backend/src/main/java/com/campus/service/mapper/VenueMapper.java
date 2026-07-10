package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {
    @Select("""
            SELECT id, name, location, capacity, status, description
            FROM venue
            WHERE id = #{id}
            FOR UPDATE
            """)
    Venue selectByIdForUpdate(@Param("id") Long id);
}
