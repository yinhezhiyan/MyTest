package com.example.mapper;

import com.example.entity.UserBehavior;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserBehaviorMapper {
    int insert(UserBehavior behavior);

    @Select("select ub.*, e.title as exercise_title from user_behavior ub left join exercise e on ub.exercise_id=e.id where ub.user_id = #{userId} order by ub.id desc")
    List<UserBehavior> selectByUserId(Integer userId);

    @Select("select * from user_behavior")
    List<UserBehavior> selectAll();
}
