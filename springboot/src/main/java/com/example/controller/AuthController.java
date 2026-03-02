package com.example.controller;

import cn.hutool.core.util.ObjectUtil;
import com.example.common.Result;
import com.example.entity.User;
import com.example.service.UserService;
import com.example.utils.JwtTokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private UserService userService;
    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (ObjectUtil.isEmpty(user.getPassword())) {
            user.setPassword("123456");
        }
        User db = userService.register(user);
        return Result.success(db);
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User db = userService.login(user.getUsername(), user.getPassword());
        String token = jwtTokenUtils.generateToken(db.getId(), db.getUsername(), db.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", db);
        return Result.success(data);
    }
}
