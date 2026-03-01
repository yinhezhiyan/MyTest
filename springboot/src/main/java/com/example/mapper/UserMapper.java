package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {
    int insert(User user);
    int updateById(User user);
    int deleteById(Integer id);
    User selectById(Integer id);
    List<User> selectAll(User user);

    @Select("select * from `sys_user` where username = #{username} limit 1")
    User selectByUsername(String username);
}
