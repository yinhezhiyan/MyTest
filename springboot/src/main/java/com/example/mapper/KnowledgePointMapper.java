package com.example.mapper;

import com.example.entity.KnowledgePoint;

import java.util.List;

public interface KnowledgePointMapper {
    int insert(KnowledgePoint knowledgePoint);
    int updateById(KnowledgePoint knowledgePoint);
    int deleteById(Integer id);
    KnowledgePoint selectById(Integer id);
    List<KnowledgePoint> selectAll(KnowledgePoint knowledgePoint);
}
