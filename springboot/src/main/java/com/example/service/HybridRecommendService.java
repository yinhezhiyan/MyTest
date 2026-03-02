package com.example.service;

import com.example.dto.RecommendationItem;
import com.example.entity.Exercise;
import com.example.entity.RecommendationLog;
import com.example.mapper.RecommendationLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HybridRecommendService {
    @Resource
    private CollaborativeFilteringService collaborativeFilteringService;
    @Resource
    private KnowledgeGraphService knowledgeGraphService;
    @Resource
    private ExerciseService exerciseService;
    @Resource
    private RecommendationLogMapper recommendationLogMapper;

    public List<RecommendationItem> recommend(Integer userId, int limit) {
        Map<Integer, Double> cf = collaborativeFilteringService.scoreByItemCF(userId);
        Map<Integer, Double> kg = knowledgeGraphService.scoreByWeakKnowledge(userId);
        Map<Integer, Exercise> exerciseMap = exerciseService.selectAll(new Exercise()).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e, (a, b) -> a));

        Set<Integer> candidateIds = new HashSet<>();
        candidateIds.addAll(cf.keySet());
        candidateIds.addAll(kg.keySet());

        List<RecommendationItem> result = new ArrayList<>();
        for (Integer id : candidateIds) {
            RecommendationItem item = new RecommendationItem();
            item.setExercise(exerciseMap.get(id));
            item.setCfScore(cf.getOrDefault(id, 0.0));
            item.setKgScore(kg.getOrDefault(id, 0.0));
            item.setHybridScore(item.getCfScore() * 0.7 + item.getKgScore() * 0.3);
            item.setReason(item.getKgScore() > 0 ? "知识点薄弱补强 + 协同过滤" : "协同过滤相似用户推荐");
            if (item.getExercise() != null) {
                result.add(item);
            }
        }

        result.sort(Comparator.comparing(RecommendationItem::getHybridScore).reversed());
        if (result.size() > limit) {
            result = result.subList(0, limit);
        }

        for (RecommendationItem item : result) {
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setExerciseId(item.getExercise().getId());
            log.setCfScore(item.getCfScore());
            log.setKgScore(item.getKgScore());
            log.setHybridScore(item.getHybridScore());
            log.setReason(item.getReason());
            recommendationLogMapper.insert(log);
        }

        return result;
    }

    public List<RecommendationLog> logs(Integer userId) {
        if (userId == null) {
            return recommendationLogMapper.selectAll();
        }
        return recommendationLogMapper.selectByUserId(userId);
    }
}
