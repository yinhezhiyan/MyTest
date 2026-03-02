package com.example.controller;

import com.example.common.Result;
import com.example.entity.Exercise;
import com.example.entity.KnowledgePoint;
import com.example.entity.User;
import com.example.service.ExerciseService;
import com.example.service.HybridRecommendService;
import com.example.service.KnowledgePointService;
import com.example.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    @Resource
    private UserService userService;
    @Resource
    private ExerciseService exerciseService;
    @Resource
    private KnowledgePointService knowledgePointService;
    @Resource
    private HybridRecommendService hybridRecommendService;

    @GetMapping("/users")
    public Result users(User user, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(userService.selectPage(user, pageNum, pageSize));
    }

    @PostMapping("/users")
    public Result addUser(@RequestBody User user) { userService.add(user); return Result.success(); }

    @PutMapping("/users")
    public Result updateUser(@RequestBody User user) { userService.updateById(user); return Result.success(); }

    @DeleteMapping("/users/{id}")
    public Result deleteUser(@PathVariable Integer id) { userService.deleteById(id); return Result.success(); }

    @GetMapping("/exercises")
    public Result exercises(Exercise exercise, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Exercise> page = exerciseService.selectPage(exercise, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/exercises")
    public Result addExercise(@RequestBody Exercise exercise) { exerciseService.add(exercise); return Result.success(); }

    @PutMapping("/exercises")
    public Result updateExercise(@RequestBody Exercise exercise) { exerciseService.updateById(exercise); return Result.success(); }

    @DeleteMapping("/exercises/{id}")
    public Result deleteExercise(@PathVariable Integer id) { exerciseService.deleteById(id); return Result.success(); }

    @GetMapping("/knowledge-points")
    public Result knowledgePoints(KnowledgePoint knowledgePoint) { return Result.success(knowledgePointService.selectAll(knowledgePoint)); }

    @PostMapping("/knowledge-points")
    public Result addKnowledgePoint(@RequestBody KnowledgePoint knowledgePoint) { knowledgePointService.add(knowledgePoint); return Result.success(); }

    @PutMapping("/knowledge-points")
    public Result updateKnowledgePoint(@RequestBody KnowledgePoint knowledgePoint) { knowledgePointService.updateById(knowledgePoint); return Result.success(); }

    @DeleteMapping("/knowledge-points/{id}")
    public Result deleteKnowledgePoint(@PathVariable Integer id) { knowledgePointService.deleteById(id); return Result.success(); }

    @GetMapping("/recommendation-logs")
    public Result recommendationLogs() { return Result.success(hybridRecommendService.logs(null)); }
}
