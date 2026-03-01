package com.example.controller;

import com.example.common.Result;
import com.example.entity.Exercise;
import com.example.service.ExerciseService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    @Resource
    private ExerciseService exerciseService;

    @PostMapping
    public Result add(@RequestBody Exercise exercise) { exerciseService.add(exercise); return Result.success(); }

    @PutMapping
    public Result update(@RequestBody Exercise exercise) { exerciseService.updateById(exercise); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) { exerciseService.deleteById(id); return Result.success(); }

    @GetMapping("/{id}")
    public Result byId(@PathVariable Integer id) { return Result.success(exerciseService.selectById(id)); }

    @GetMapping("/page")
    public Result page(Exercise exercise,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Exercise> page = exerciseService.selectPage(exercise, pageNum, pageSize);
        return Result.success(page);
    }
}
