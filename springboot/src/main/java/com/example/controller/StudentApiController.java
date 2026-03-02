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

    @GetMapping("/exercises/{id}")
    public Result exerciseDetail(@PathVariable Integer id, HttpServletRequest request) {
        Exercise e = exerciseService.selectById(id);
        if (e == null || !AuthUtils.currentUserSubject(request).equals(e.getSubject())) return Result.error("无权访问该学科习题");
        return Result.success(e);
    }

    @PostMapping("/behaviors/answer")
    public Result answer(@RequestBody UserBehavior behavior, HttpServletRequest request) {
        behavior.setUserId(AuthUtils.currentUserId(request));
        behavior.setActionType("ANSWER");
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
