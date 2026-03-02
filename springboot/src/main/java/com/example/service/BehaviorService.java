package com.example.service;

import com.example.dto.LearningOverviewDTO;
import com.example.entity.RecommendationLog;
import com.example.entity.UserBehavior;
import com.example.mapper.RecommendationLogMapper;
import com.example.mapper.UserBehaviorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BehaviorService {
    @Resource
    private UserBehaviorMapper userBehaviorMapper;
    @Resource
    private RecommendationLogMapper recommendationLogMapper;

    public void recordAnswer(UserBehavior behavior) {
        if (behavior.getActionType() == null) {
            behavior.setActionType("ANSWER");
        }
        userBehaviorMapper.insert(behavior);
    }

    public List<UserBehavior> selectByUserId(Integer userId) {
        return userBehaviorMapper.selectByUserId(userId);
    }

    public List<UserBehavior> selectAll() {
        return userBehaviorMapper.selectAll();
    }

    public LearningOverviewDTO learningOverview(Integer userId) {
        List<UserBehavior> list = userBehaviorMapper.selectByUserId(userId);
        int total = list.size();
        int correct = (int) list.stream().filter(b -> b.getIsCorrect() != null && b.getIsCorrect() == 1).count();
        double accuracy = total == 0 ? 0D : (double) correct / total;
        List<RecommendationLog> logs = recommendationLogMapper.selectByUserId(userId);

        LearningOverviewDTO dto = new LearningOverviewDTO();
        dto.setTotalAnswers(total);
        dto.setCorrectAnswers(correct);
        dto.setAccuracy(Math.round(accuracy * 10000) / 100.0);
        dto.setRecommendedCount(logs.size());
        return dto;
    }
}

