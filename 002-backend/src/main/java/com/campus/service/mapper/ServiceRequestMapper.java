package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.ServiceRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ServiceRequestMapper extends BaseMapper<ServiceRequest> {
    @Select("""
            SELECT id
            FROM service_request
            WHERE venue_id = #{venueId}
              AND deleted = 0
              AND status NOT IN ('REJECTED', 'CANCELLED')
              AND appointment_start < #{appointmentEnd}
              AND appointment_end > #{appointmentStart}
            LIMIT 1
            FOR UPDATE
            """)
    Long findOverlappingVenueRequestForUpdate(@Param("venueId") Long venueId,
                                               @Param("appointmentStart") LocalDateTime appointmentStart,
                                               @Param("appointmentEnd") LocalDateTime appointmentEnd);
}
