package com.example.controller;

import com.example.common.Result;
import com.example.service.StudentService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/student")
public class StudentController {

    @Resource
    private StudentService studentService;

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String name) {
        return Result.success(studentService.selectPage(pageNum, pageSize, name));
    }

    @GetMapping("/records/{studentId}")
    public Result records(@PathVariable Integer studentId,
                          @RequestParam(required = false) String date) {
        return Result.success(studentService.records(studentId, date));
    }
}
