package com.example.service;

import com.example.entity.Exercise;
import com.example.mapper.ExerciseMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseService {
    @Resource
    private ExerciseMapper exerciseMapper;

    public void add(Exercise exercise) { exerciseMapper.insert(exercise); }
    public void updateById(Exercise exercise) { exerciseMapper.updateById(exercise); }
    public void deleteById(Integer id) { exerciseMapper.deleteById(id); }
    public Exercise selectById(Integer id) { return exerciseMapper.selectById(id); }
    public Exercise selectRandomBySubject(String subject) { return exerciseMapper.selectRandomBySubject(subject); }
    public List<Exercise> selectAll(Exercise exercise) { return exerciseMapper.selectAll(exercise); }
    public PageInfo<Exercise> selectPage(Exercise exercise, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(exerciseMapper.selectAll(exercise));
    }
}
