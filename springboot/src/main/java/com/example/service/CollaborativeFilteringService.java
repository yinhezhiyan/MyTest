package com.example.service;

import com.example.entity.UserBehavior;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollaborativeFilteringService {
    @Resource
    private BehaviorService behaviorService;

    public Map<Integer, Double> scoreByItemCF(Integer userId) {
        List<UserBehavior> all = behaviorService.selectAll();
        Set<Integer> solved = all.stream().filter(b -> userId.equals(b.getUserId()) && b.getIsCorrect() != null && b.getIsCorrect() == 1)
                .map(UserBehavior::getExerciseId).collect(Collectors.toSet());
        Set<Integer> peers = all.stream().filter(b -> solved.contains(b.getExerciseId()) && !userId.equals(b.getUserId()))
                .map(UserBehavior::getUserId).collect(Collectors.toSet());

        Map<Integer, Double> scores = new HashMap<>();
        for (UserBehavior b : all) {
            if (peers.contains(b.getUserId()) && (b.getIsCorrect() != null && b.getIsCorrect() == 1) && !solved.contains(b.getExerciseId())) {
                scores.merge(b.getExerciseId(), 1.0, Double::sum);
            }
        }
        return scores;
    }
}
