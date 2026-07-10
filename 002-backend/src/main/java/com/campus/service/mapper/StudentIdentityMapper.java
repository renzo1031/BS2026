package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.StudentIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentIdentityMapper extends BaseMapper<StudentIdentity> {
    @Select("select * from student_identity where student_no = #{studentNo} limit 1 for update")
    StudentIdentity findByStudentNoForUpdate(String studentNo);
}
