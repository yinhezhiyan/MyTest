package com.example.mapper;

import com.example.entity.Exercise;

import java.util.List;

public interface ExerciseMapper {
    int insert(Exercise exercise);
    int updateById(Exercise exercise);
    int deleteById(Integer id);
    Exercise selectById(Integer id);
    List<Exercise> selectAll(Exercise exercise);
    Exercise selectRandomBySubject(String subject);
}
