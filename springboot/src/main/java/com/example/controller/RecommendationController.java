package com.example.controller;

import com.example.common.Result;
import com.example.service.HybridRecommendService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.utils.AuthUtils;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    @Resource
    private HybridRecommendService hybridRecommendService;

    @GetMapping
    public Result recommend(HttpServletRequest request, @RequestParam(defaultValue = "10") Integer limit) {
        Integer userId = AuthUtils.currentUserId(request);
        return Result.success(hybridRecommendService.recommend(userId, limit));
    }

    @GetMapping("/logs")
    public Result logs(HttpServletRequest request, @RequestParam(required = false) Integer userId) {
        if (!"ADMIN".equals(AuthUtils.currentUserRole(request))) {
            userId = AuthUtils.currentUserId(request);
        }
        return Result.success(hybridRecommendService.logs(userId));
    }
}
