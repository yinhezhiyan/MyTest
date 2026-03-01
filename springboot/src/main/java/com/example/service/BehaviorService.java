package com.example.service;

import com.example.entity.UserBehavior;
import com.example.mapper.UserBehaviorMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BehaviorService {
    @Resource
    private UserBehaviorMapper userBehaviorMapper;

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
}
