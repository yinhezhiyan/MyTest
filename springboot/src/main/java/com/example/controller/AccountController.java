package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Resource
    private AccountService accountService;

    @GetMapping("/profile")
    public Result profile() {
        return Result.success(accountService.profile());
    }

    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Account account) {
        return Result.success(accountService.updateProfile(account));
    }
}
