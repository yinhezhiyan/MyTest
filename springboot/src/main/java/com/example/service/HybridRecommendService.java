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

    public List<RecommendationItem> recommend(Integer userId, String subject, int limit) {
        Map<Integer, Double> cf = collaborativeFilteringService.scoreByItemCF(userId);
        Map<Integer, Double> kg = knowledgeGraphService.scoreByWeakKnowledge(userId, subject);

        Exercise probe = new Exercise();
        probe.setSubject(subject);
        List<Exercise> subjectExercises = exerciseService.selectAll(probe);
        Map<Integer, Exercise> exerciseMap = subjectExercises.stream().collect(Collectors.toMap(Exercise::getId, e -> e, (a, b) -> a));

        Set<Integer> candidateIds = new HashSet<>();
        candidateIds.addAll(cf.keySet());
        candidateIds.addAll(kg.keySet());

        List<RecommendationItem> result = new ArrayList<>();
        for (Integer id : candidateIds) {
            if (!exerciseMap.containsKey(id)) continue;
            RecommendationItem item = new RecommendationItem();
            item.setExercise(exerciseMap.get(id));
            item.setCfScore(cf.getOrDefault(id, 0.0));
            item.setKgScore(kg.getOrDefault(id, 0.0));
            item.setHybridScore(item.getCfScore() * 0.7 + item.getKgScore() * 0.3);
            item.setReason(item.getKgScore() > 0 ? "KG: 你薄弱知识点的前置练习 + CF: 相似用户做过" : "CF: 相似用户做过");
            result.add(item);
        }

        if (result.isEmpty()) {
            for (int i = 0; i < Math.min(limit, subjectExercises.size()); i++) {
                RecommendationItem item = new RecommendationItem();
                item.setExercise(subjectExercises.get(i));
                item.setCfScore(0.2);
                item.setKgScore(0.2);
                item.setHybridScore(0.2);
                item.setReason("兜底推荐: 当前学科热门题目");
                result.add(item);
            }
        }

        result.sort(Comparator.comparing(RecommendationItem::getHybridScore).reversed());
        if (result.size() > limit) result = result.subList(0, limit);

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
        return userId == null ? recommendationLogMapper.selectAll() : recommendationLogMapper.selectByUserId(userId);
    }
}
