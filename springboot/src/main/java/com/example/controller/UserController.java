package com.example.controller;

import com.example.common.Result;
import com.example.entity.User;
import com.example.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping
    public Result add(@RequestBody User user) { userService.add(user); return Result.success(); }

    @PutMapping
    public Result update(@RequestBody User user) { userService.updateById(user); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) { userService.deleteById(id); return Result.success(); }

    @GetMapping("/{id}")
    public Result byId(@PathVariable Integer id) { return Result.success(userService.selectById(id)); }

    @GetMapping("/page")
    public Result page(User user,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<User> page = userService.selectPage(user, pageNum, pageSize);
        return Result.success(page);
    }
}
