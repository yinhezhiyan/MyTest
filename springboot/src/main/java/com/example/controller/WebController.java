package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.service.AdminService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebController {

    @Resource
    private AdminService adminService;

    @GetMapping("/")
    public Result hello() {
        return Result.success();
    }

    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        Account ac = adminService.login(account);
        return Result.success(ac);
    }

    @PostMapping("/register")
    public Result register(@RequestBody Account account) {
        if (!"STUDENT".equals(account.getRole())) {
            return Result.error("管理员不可注册，请联系系统管理员创建账号");
        }
        adminService.registerStudent(account);
        return Result.success();
    }

    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        adminService.updatePassword(account);
        return Result.success();
    }
}
