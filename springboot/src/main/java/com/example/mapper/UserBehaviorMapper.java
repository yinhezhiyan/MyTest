package com.example.mapper;

import com.example.entity.UserBehavior;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserBehaviorMapper {
    int insert(UserBehavior behavior);

    @Select("select * from user_behavior where user_id = #{userId}")
    List<UserBehavior> selectByUserId(Integer userId);

    @Select("select * from user_behavior")
    List<UserBehavior> selectAll();
}
