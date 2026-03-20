package com.example.controller;

import com.example.common.Result;
import com.example.service.KnowledgeGraphService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @GetMapping("/api/knowledge-graph/me")
    public Result myKnowledgeGraph() {
        return Result.success(knowledgeGraphService.currentStudentKnowledgeGraph());
    }

    @GetMapping("/admin/student/{studentId}/knowledge-graph")
    public Result studentKnowledgeGraph(@PathVariable Integer studentId) {
        return Result.success(knowledgeGraphService.studentKnowledgeGraphForAdmin(studentId));
    }

    @GetMapping("/admin/knowledge-graph/overview")
    public Result adminKnowledgeGraph() {
        return Result.success(knowledgeGraphService.adminSubjectKnowledgeGraph());
    }

    @GetMapping("/admin/knowledge-graph/relations")
    public Result relationList() {
        return Result.success(knowledgeGraphService.relationList());
    }

    @GetMapping("/admin/knowledge-graph/points")
    public Result pointList() {
        return Result.success(knowledgeGraphService.pointList());
    }

    @PutMapping("/admin/knowledge-graph/points/{id}/weight")
    public Result updatePointWeight(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        knowledgeGraphService.updatePointWeight(id, payload.get("weight"));
        return Result.success();
    }

    @PostMapping("/admin/knowledge-graph/relations")
    public Result createRelation(@RequestBody Map<String, Object> payload) {
        knowledgeGraphService.saveRelation(payload);
        return Result.success();
    }

    @PostMapping("/admin/knowledge-graph/relations/batch")
    public Result createRelations(@RequestBody java.util.List<Map<String, Object>> payloads) {
        knowledgeGraphService.saveRelations(payloads);
        return Result.success();
    }

    @PutMapping("/admin/knowledge-graph/relations/{id}")
    public Result updateRelation(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        knowledgeGraphService.updateRelation(id, payload);
        return Result.success();
    }

    @DeleteMapping("/admin/knowledge-graph/relations/{id}")
    public Result deleteRelation(@PathVariable Long id) {
        knowledgeGraphService.deleteRelation(id);
        return Result.success();
    }
}
