package com.example.mapper;

import com.example.entity.RecommendationLog;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RecommendationLogMapper {
    int insert(RecommendationLog recommendationLog);

    @Select("select * from recommendation_log where user_id = #{userId} order by id desc")
    List<RecommendationLog> selectByUserId(Integer userId);

    @Select("select * from recommendation_log order by id desc")
    List<RecommendationLog> selectAll();
}
