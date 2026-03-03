package com.example.controller;

import com.example.common.Result;
import com.example.dto.ImportRequest;
import com.example.dto.SubmitAnswerRequest;
import com.example.entity.Exercise;
import com.example.context.UserContext;
import com.example.service.ExerciseService;
import org.springframework.web.bind.annotation.*;


@RestController
public class ExerciseController {
    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @PostMapping("/admin/question-bank/import")
    public Result importBank(@RequestBody ImportRequest request) {
        return Result.success(exerciseService.importFromJson(request.getSubject(), request.getFilePath()));
    }


    @PostMapping("/admin/question-bank/importCurrent")
    public Result importCurrent() {
        String subject = UserContext.get().getSubject();
        return Result.success(exerciseService.importFromJson(subject, null));
    }


    @GetMapping("/admin/question-bank/chapters")
    public Result chapterBank() {
        return Result.success(exerciseService.chapterBank());
    }

    @PostMapping("/admin/question-bank/exercise")
    public Result addExercise(@RequestBody Exercise exercise) {
        exerciseService.addExerciseByAdmin(exercise);
        return Result.success();
    }

    @DeleteMapping("/admin/question-bank/exercise/{id}")
    public Result deleteExercise(@PathVariable String id) {
        exerciseService.deleteExerciseByAdmin(id);
        return Result.success();
    }

    @GetMapping("/api/profile/summary")
    public Result studentSummary() {
        return Result.success(exerciseService.studentHomeSummary());
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
                          @RequestParam(required = false) Integer correct,
                          @RequestParam(required = false) String date) {
        return Result.success(exerciseService.answerRecords(chapter, correct, date));
    }

    @GetMapping("/api/recommendations")
    public Result recommendations(@RequestParam(defaultValue = "5") Integer topN,
                                  @RequestParam(defaultValue = "false") Boolean includeDone) {
        return Result.success(exerciseService.recommendations(topN, includeDone));
    }

    @GetMapping("/api/daily")
    public Result daily() {
        return Result.success(exerciseService.dailyUnseen(5));
    }
}
