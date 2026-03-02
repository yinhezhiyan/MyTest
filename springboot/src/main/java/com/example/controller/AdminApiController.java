package com.example.controller;

import com.example.common.Result;
import com.example.entity.Exercise;
import com.example.entity.KnowledgePoint;
import com.example.entity.User;
import com.example.service.ExerciseService;
import com.example.service.HybridRecommendService;
import com.example.service.KnowledgePointService;
import com.example.service.UserService;
import com.example.utils.AuthUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result users(User user, HttpServletRequest request, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        user.setSubject(AuthUtils.currentUserSubject(request));
        return Result.success(userService.selectPage(user, pageNum, pageSize));
    }

    @PostMapping("/users")
    public Result addUser(@RequestBody User user, HttpServletRequest request) { user.setSubject(AuthUtils.currentUserSubject(request)); userService.add(user); return Result.success(); }
    @PutMapping("/users")
    public Result updateUser(@RequestBody User user, HttpServletRequest request) { user.setSubject(AuthUtils.currentUserSubject(request)); userService.updateById(user); return Result.success(); }
    @DeleteMapping("/users/{id}")
    public Result deleteUser(@PathVariable Integer id) { userService.deleteById(id); return Result.success(); }

    @GetMapping("/exercises")
    public Result exercises(Exercise exercise, HttpServletRequest request, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        exercise.setSubject(AuthUtils.currentUserSubject(request));
        PageInfo<Exercise> page = exerciseService.selectPage(exercise, pageNum, pageSize);
        return Result.success(page);
    }

    @PostMapping("/exercises")
    public Result addExercise(@RequestBody Exercise exercise, HttpServletRequest request) { exercise.setSubject(AuthUtils.currentUserSubject(request)); exerciseService.add(exercise); return Result.success(); }
    @PutMapping("/exercises")
    public Result updateExercise(@RequestBody Exercise exercise, HttpServletRequest request) { exercise.setSubject(AuthUtils.currentUserSubject(request)); exerciseService.updateById(exercise); return Result.success(); }
    @DeleteMapping("/exercises/{id}")
    public Result deleteExercise(@PathVariable Integer id) { exerciseService.deleteById(id); return Result.success(); }

    @GetMapping("/knowledge-points")
    public Result knowledgePoints(KnowledgePoint knowledgePoint, HttpServletRequest request) { knowledgePoint.setSubject(AuthUtils.currentUserSubject(request)); return Result.success(knowledgePointService.selectAll(knowledgePoint)); }
    @PostMapping("/knowledge-points")
    public Result addKnowledgePoint(@RequestBody KnowledgePoint knowledgePoint, HttpServletRequest request) { knowledgePoint.setSubject(AuthUtils.currentUserSubject(request)); knowledgePointService.add(knowledgePoint); return Result.success(); }
    @PutMapping("/knowledge-points")
    public Result updateKnowledgePoint(@RequestBody KnowledgePoint knowledgePoint, HttpServletRequest request) { knowledgePoint.setSubject(AuthUtils.currentUserSubject(request)); knowledgePointService.updateById(knowledgePoint); return Result.success(); }
    @DeleteMapping("/knowledge-points/{id}")
    public Result deleteKnowledgePoint(@PathVariable Integer id) { knowledgePointService.deleteById(id); return Result.success(); }

    @GetMapping("/recommendation-logs")
    public Result recommendationLogs() { return Result.success(hybridRecommendService.logs(null)); }
}
