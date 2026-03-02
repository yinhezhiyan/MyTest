package com.example.service;

import com.example.entity.Exercise;
import com.example.entity.UserBehavior;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {
    @Resource
    private ExerciseService exerciseService;
    @Resource
    private BehaviorService behaviorService;

    public Map<Integer, Double> scoreByWeakKnowledge(Integer userId) {
        List<UserBehavior> mine = behaviorService.selectByUserId(userId);
        Set<Integer> wrongExerciseIds = mine.stream()
                .filter(b -> b.getIsCorrect() != null && b.getIsCorrect() == 0)
                .map(UserBehavior::getExerciseId)
                .collect(Collectors.toSet());

        List<Exercise> all = exerciseService.selectAll(new Exercise());
        Set<Integer> weakKnowledge = all.stream().filter(e -> wrongExerciseIds.contains(e.getId()) && e.getKnowledgePointId() != null)
                .map(Exercise::getKnowledgePointId).collect(Collectors.toSet());

        Map<Integer, Double> result = new HashMap<>();
        for (Exercise exercise : all) {
            if (exercise.getKnowledgePointId() != null && weakKnowledge.contains(exercise.getKnowledgePointId())) {
                result.put(exercise.getId(), 1.0);
            }
        }
        return result;
    }
}
