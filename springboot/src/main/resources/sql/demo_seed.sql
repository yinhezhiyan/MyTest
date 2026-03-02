-- 演示版初始化脚本（MySQL 8+）
CREATE TABLE IF NOT EXISTS sys_user (
  id INT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  name VARCHAR(64),
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
  avatar VARCHAR(255),
  grade VARCHAR(32)
);

ALTER TABLE sys_user MODIFY COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STUDENT';

CREATE TABLE IF NOT EXISTS knowledge_point (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(255),
  parent_id INT NULL
);

CREATE TABLE IF NOT EXISTS exercise (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT,
  difficulty VARCHAR(20),
  knowledge_point_id INT,
  file_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_behavior (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  exercise_id INT NOT NULL,
  action_type VARCHAR(30) NOT NULL,
  is_correct TINYINT,
  score INT
);

CREATE TABLE IF NOT EXISTS recommendation_log (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NOT NULL,
  exercise_id INT NOT NULL,
  cf_score DECIMAL(10,2),
  kg_score DECIMAL(10,2),
  hybrid_score DECIMAL(10,2),
  reason VARCHAR(255)
);

DELETE FROM recommendation_log;
DELETE FROM user_behavior;
DELETE FROM exercise;
DELETE FROM knowledge_point;
DELETE FROM sys_user;

INSERT INTO sys_user (id, username, password, name, role, grade) VALUES
(1, 'admin', 'admin123', '系统管理员', 'ADMIN', NULL),
(2, 'stu_zhang', '123456', '张同学', 'STUDENT', '大二'),
(3, 'stu_li', '123456', '李同学', 'STUDENT', '大三'),
(4, 'stu_wang', '123456', '王同学', 'STUDENT', '大二'),
(5, 'stu_zhao', '123456', '赵同学', 'STUDENT', '大一');

INSERT INTO knowledge_point (id, name, description, parent_id) VALUES
(1, '协同过滤', '基于行为相似度进行推荐', NULL),
(2, '知识图谱', '基于知识关联进行推理推荐', NULL),
(3, '相似度计算', '余弦相似度/皮尔逊相关系数', 1),
(4, '图谱推理', '从薄弱知识点扩展关联题目', 2),
(5, '推荐解释', '可解释性推荐文本生成', 2),
(6, '混合策略', 'CF 与 KG 融合打分', 1);

INSERT INTO exercise (id, title, content, difficulty, knowledge_point_id) VALUES
(1, '协同过滤基础概念', '说明UserCF与ItemCF的区别。', '简单', 1),
(2, '余弦相似度练习', '计算两个用户向量的余弦相似度。', '中等', 3),
(3, '知识图谱实体关系建模', '设计习题-知识点图谱结构。', '中等', 2),
(4, '图谱路径推理题', '给定弱项知识点，推导可推荐题目集合。', '困难', 4),
(5, '混合推荐融合策略', '设计CF与KG融合评分公式。', '中等', 6),
(6, '推荐解释性分析', '给出推荐理由生成逻辑。', '简单', 5),
(7, '用户画像建模', '基于行为构建学习画像字段。', '中等', 1),
(8, '知识点先修关系判断', '判断图中先修关系是否存在环。', '困难', 2),
(9, '稀疏矩阵处理', '如何处理冷启动与稀疏问题。', '中等', 3),
(10, '推荐结果评估', '设计准确率与召回率计算过程。', '简单', 5);

INSERT INTO user_behavior (user_id, exercise_id, action_type, is_correct, score) VALUES
(2, 1, 'ANSWER', 1, 90),(2, 2, 'ANSWER', 0, 50),(2, 3, 'ANSWER', 0, 45),(2, 6, 'ANSWER', 1, 82),
(3, 1, 'ANSWER', 1, 88),(3, 2, 'ANSWER', 1, 80),(3, 5, 'ANSWER', 1, 86),(3, 9, 'ANSWER', 1, 84),
(4, 1, 'ANSWER', 1, 91),(4, 3, 'ANSWER', 1, 85),(4, 6, 'ANSWER', 1, 89),(4, 10, 'ANSWER', 1, 90),
(5, 2, 'ANSWER', 0, 48),(5, 4, 'ANSWER', 0, 40),(5, 8, 'ANSWER', 0, 35);
