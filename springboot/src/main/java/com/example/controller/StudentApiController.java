package com.example.controller;

import com.example.common.Result;
import com.example.dto.RecommendationItem;
import com.example.entity.Exercise;
import com.example.entity.UserBehavior;
import com.example.service.BehaviorService;
import com.example.service.ExerciseService;
import com.example.service.HybridRecommendService;
import com.example.utils.AuthUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentApiController {
    @Resource
    private HybridRecommendService hybridRecommendService;
    @Resource
    private ExerciseService exerciseService;
    @Resource
    private BehaviorService behaviorService;

    @GetMapping("/recommendations")
    public Result recommendations(HttpServletRequest request, @RequestParam(defaultValue = "10") Integer limit) {
        Integer userId = AuthUtils.currentUserId(request);
        String subject = AuthUtils.currentUserSubject(request);
        List<RecommendationItem> items = hybridRecommendService.recommend(userId, subject, limit);
        for (RecommendationItem item : items) item.setReason(item.getReason() + " | HybridScore: " + item.getHybridScore());
        return Result.success(items);
    }

    @GetMapping("/extend/today")
    public Result todayExtend(HttpServletRequest request, @RequestParam(defaultValue = "8") Integer size) {
        Integer userId = AuthUtils.currentUserId(request);
        String subject = AuthUtils.currentUserSubject(request);
        List<RecommendationItem> items = hybridRecommendService.recommend(userId, subject, size);
        return Result.success(items);
    }

    @GetMapping("/exercises/{id}")
    public Result exerciseDetail(@PathVariable Integer id, HttpServletRequest request) {
        Exercise e = exerciseService.selectById(id);
        if (e == null || !AuthUtils.currentUserSubject(request).equals(e.getSubject())) return Result.error("无权访问该学科习题");
        e.setAnswer(null);
        return Result.success(e);
    }

    @GetMapping("/practice/next")
    public Result nextPractice(HttpServletRequest request) {
        String subject = AuthUtils.currentUserSubject(request);
        Exercise e = exerciseService.selectRandomBySubject(subject);
        if (e == null) return Result.error("当前学科暂无题目");
        e.setAnswer(null);
        return Result.success(e);
    }

    @PostMapping("/attempt")
    public Result attempt(@RequestBody UserBehavior behavior, HttpServletRequest request) {
        Integer userId = AuthUtils.currentUserId(request);
        String subject = AuthUtils.currentUserSubject(request);
        Exercise e = exerciseService.selectById(behavior.getExerciseId());
        if (e == null || !subject.equals(e.getSubject())) return Result.error("无权访问该题目");

        boolean correct = behavior.getChosen() != null && behavior.getChosen().equalsIgnoreCase(e.getAnswer());
        UserBehavior record = new UserBehavior();
        record.setUserId(userId);
        record.setExerciseId(behavior.getExerciseId());
        record.setActionType("ANSWER");
        record.setIsCorrect(correct ? 1 : 0);
        record.setScore(correct ? 100 : 0);
        record.setChosen(behavior.getChosen());
        record.setSubject(subject);
        behaviorService.recordAnswer(record);

        Map<String, Object> data = new HashMap<>();
        data.put("isCorrect", correct);
        data.put("correctAnswer", e.getAnswer());
        data.put("analysis", e.getAnalysis());
        return Result.success(data);
    }

    @PostMapping("/behaviors/answer")
    public Result answer(@RequestBody UserBehavior behavior, HttpServletRequest request) {
        behavior.setUserId(AuthUtils.currentUserId(request));
        behavior.setActionType("ANSWER");
        behavior.setSubject(AuthUtils.currentUserSubject(request));
        behaviorService.recordAnswer(behavior);
        return Result.success();
    }

    @GetMapping("/behaviors")
    public Result myBehaviors(HttpServletRequest request) {
        Integer userId = AuthUtils.currentUserId(request);
        List<UserBehavior> list = behaviorService.selectByUserId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data);
    }
}
