package com.example.service;

import com.example.entity.KnowledgePoint;
import com.example.mapper.KnowledgePointMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgePointService {
    @Resource
    private KnowledgePointMapper knowledgePointMapper;

    public void add(KnowledgePoint knowledgePoint) { knowledgePointMapper.insert(knowledgePoint); }
    public void updateById(KnowledgePoint knowledgePoint) { knowledgePointMapper.updateById(knowledgePoint); }
    public void deleteById(Integer id) { knowledgePointMapper.deleteById(id); }
    public List<KnowledgePoint> selectAll(KnowledgePoint knowledgePoint) { return knowledgePointMapper.selectAll(knowledgePoint); }
}
