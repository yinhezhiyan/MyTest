package com.example.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.utils.AuthUtils;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping
    public Result add(@RequestBody User user, HttpServletRequest request) { AuthUtils.requireAdmin(request); userService.add(user); return Result.success(); }

    @PutMapping
    public Result update(@RequestBody User user, HttpServletRequest request) { AuthUtils.requireAdmin(request); userService.updateById(user); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id, HttpServletRequest request) { AuthUtils.requireAdmin(request); userService.deleteById(id); return Result.success(); }

    @GetMapping("/{id}")
    public Result byId(@PathVariable Integer id) { return Result.success(userService.selectById(id)); }

    @GetMapping("/page")
    public Result page(User user,
                       HttpServletRequest request,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        AuthUtils.requireAdmin(request);
        PageInfo<User> page = userService.selectPage(user, pageNum, pageSize);
        return Result.success(page);
    }
}
