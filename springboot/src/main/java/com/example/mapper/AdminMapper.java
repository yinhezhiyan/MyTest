package com.example.mapper;

import com.example.entity.Admin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AdminMapper {

    int insert(Admin admin);

    int deleteById(Integer id);

    int updateById(Admin admin);

    Admin selectById(Integer id);

    List<Admin> selectAll(Admin admin);

    @Select("select id, username, password, name, avatar, role, subject from sys_user where username = #{username} and role = #{role} and subject = #{subject}")
    Admin selectByIdentity(@Param("username") String username, @Param("role") String role, @Param("subject") String subject);
}
