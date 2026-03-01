package com.example.controller;

import com.example.common.Result;
import com.example.entity.KnowledgePoint;
import com.example.service.KnowledgePointService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.example.utils.AuthUtils;

@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {
    @Resource
    private KnowledgePointService knowledgePointService;

    @PostMapping
    public Result add(@RequestBody KnowledgePoint knowledgePoint, HttpServletRequest request) { AuthUtils.requireAdmin(request); knowledgePointService.add(knowledgePoint); return Result.success(); }

    @PutMapping
    public Result update(@RequestBody KnowledgePoint knowledgePoint, HttpServletRequest request) { AuthUtils.requireAdmin(request); knowledgePointService.updateById(knowledgePoint); return Result.success(); }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id, HttpServletRequest request) { AuthUtils.requireAdmin(request); knowledgePointService.deleteById(id); return Result.success(); }

    @GetMapping
    public Result list(KnowledgePoint knowledgePoint) { return Result.success(knowledgePointService.selectAll(knowledgePoint)); }
}
