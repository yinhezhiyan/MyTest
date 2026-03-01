package com.example.controller;

import com.example.common.Result;
import com.example.entity.UserBehavior;
import com.example.service.BehaviorService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.example.utils.AuthUtils;

@RestController
@RequestMapping("/api/behaviors")
public class BehaviorController {
    @Resource
    private BehaviorService behaviorService;

    @PostMapping("/answer")
    public Result answer(@RequestBody UserBehavior behavior, HttpServletRequest request) {
        if (behavior.getUserId() == null) {
            behavior.setUserId(AuthUtils.currentUserId(request));
        }
        behaviorService.recordAnswer(behavior);
        return Result.success();
    }
}
