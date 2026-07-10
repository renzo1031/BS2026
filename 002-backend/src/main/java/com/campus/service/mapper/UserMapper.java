package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from sys_user where username = #{username} and deleted = 0 limit 1")
    User findByUsername(String username);

    @Select("select * from sys_user where student_no = #{studentNo} and deleted = 0 limit 1")
    User findByStudentNo(String studentNo);
}
