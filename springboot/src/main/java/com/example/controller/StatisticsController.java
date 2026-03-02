package com.example.controller;

import com.example.common.Result;
import com.example.service.BehaviorService;
import com.example.utils.AuthUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Resource
    private BehaviorService behaviorService;

    @GetMapping("/learning-overview")
    public Result overview(HttpServletRequest request, @RequestParam(required = false) Integer userId) {
        if (!"ADMIN".equals(AuthUtils.currentUserRole(request))) {
            userId = AuthUtils.currentUserId(request);
        }
        return Result.success(behaviorService.learningOverview(userId));
    }
}
