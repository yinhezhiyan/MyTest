package com.example.controller;

import com.example.common.Result;
import com.example.dto.ImportRequest;
import com.example.dto.SubmitAnswerRequest;
import com.example.service.ExerciseService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ExerciseController {
    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PostMapping("/admin/question-bank/import")
    public Result importBank(@RequestBody ImportRequest request) {
        int count = exerciseService.importFromJson(request.getSubject(), request.getFilePath());
        return Result.success(Map.of("imported", count));
    }

    @GetMapping("/api/exercises/random")
    public Result randomExercise() {
        return Result.success(exerciseService.randomExercise());
    }

    @GetMapping("/api/exercises/{id}")
    public Result getById(@PathVariable String id) {
        return Result.success(exerciseService.getById(id));
    }

    @PostMapping("/api/answers/submit")
    public Result submit(@RequestBody SubmitAnswerRequest request) {
        return Result.success(exerciseService.submit(request));
    }

    @GetMapping("/api/answers/records")
    public Result records(@RequestParam(required = false) String chapter,
                          @RequestParam(required = false) Integer correct) {
        return Result.success(exerciseService.answerRecords(chapter, correct));
    }

    @GetMapping("/api/recommendations")
    public Result recommendations(@RequestParam(defaultValue = "5") Integer topN) {
        return Result.success(exerciseService.recommendations(topN));
    }

    @GetMapping("/api/daily")
    public Result daily() {
        return Result.success(exerciseService.recommendations(5));
    }
}
