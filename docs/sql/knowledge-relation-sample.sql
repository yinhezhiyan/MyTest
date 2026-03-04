-- 示例：知识图谱关系（可按学科逐步补全）
-- relation_type 可选：prerequisite / related / contains

INSERT INTO knowledge_relation(subject, source_kp, target_kp, relation_type, weight) VALUES
('DS', '线性表', '栈', 'prerequisite', 1.20),
('DS', '线性表', '队列', 'prerequisite', 1.20),
('DS', '树', '二叉树', 'contains', 1.00),
('DS', '二叉树', '哈夫曼树', 'prerequisite', 1.10),
('OS', '进程管理', '线程', 'contains', 1.00),
('OS', '进程调度', '死锁', 'related', 0.90),
('CN', '网络层', '路由算法', 'contains', 1.00),
('CO', '存储系统', 'Cache', 'contains', 1.00);
