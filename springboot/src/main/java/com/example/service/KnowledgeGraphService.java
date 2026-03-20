package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.context.UserContext;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeGraphService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void refreshAllSubjects(List<String> subjects) {
        for (String subject : subjects) {
            refreshKnowledgePoints(subject);
        }
    }

    public void refreshKnowledgePoints(String subject) {
        Map<String, Set<String>> chapterRefs = new HashMap<>();
        Map<String, Integer> exerciseCounts = new HashMap<>();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select chapter, knowledge_points from exercise where subject=?",
                subject
        );
        for (Map<String, Object> row : rows) {
            String chapter = Objects.toString(row.get("chapter"), "未分类章节");
            for (String kp : parseKnowledgePoints((String) row.get("knowledge_points"))) {
                chapterRefs.computeIfAbsent(kp, key -> new LinkedHashSet<>()).add(chapter);
                exerciseCounts.merge(kp, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> edges = jdbcTemplate.queryForList(
                "select source_kp, target_kp from knowledge_relation where subject=?",
                subject
        );
        for (Map<String, Object> edge : edges) {
            String source = Objects.toString(edge.get("source_kp"), "").trim();
            String target = Objects.toString(edge.get("target_kp"), "").trim();
            if (!source.isEmpty()) {
                chapterRefs.computeIfAbsent(source, key -> new LinkedHashSet<>());
                exerciseCounts.putIfAbsent(source, 0);
            }
            if (!target.isEmpty()) {
                chapterRefs.computeIfAbsent(target, key -> new LinkedHashSet<>());
                exerciseCounts.putIfAbsent(target, 0);
            }
        }

        Set<String> current = new HashSet<>(jdbcTemplate.queryForList(
                "select kp_name from knowledge_point where subject=?",
                String.class,
                subject
        ));
        Set<String> latest = chapterRefs.keySet();

        for (String kp : latest) {
            String chapterJson = writeJson(new ArrayList<>(chapterRefs.getOrDefault(kp, Set.of())));
            jdbcTemplate.update(
                    """
                    insert into knowledge_point(subject, kp_name, chapter_refs, exercise_count, source_type)
                    values(?,?,?,?,?)
                    on duplicate key update chapter_refs=values(chapter_refs), exercise_count=values(exercise_count)
                    """,
                    subject,
                    kp,
                    chapterJson,
                    exerciseCounts.getOrDefault(kp, 0),
                    exerciseCounts.getOrDefault(kp, 0) > 0 ? "EXERCISE" : "RELATION"
            );
        }

        for (String stale : current) {
            if (!latest.contains(stale)) {
                jdbcTemplate.update("delete from knowledge_point where subject=? and kp_name=?", subject, stale);
            }
        }
    }

    public Map<String, Object> currentStudentKnowledgeGraph() {
        Account current = requireLogin();
        return studentKnowledgeGraph(current.getId(), current.getSubject());
    }

    public Map<String, Object> studentKnowledgeGraphForAdmin(Integer studentId) {
        Account admin = requireAdmin();
        return studentKnowledgeGraph(studentId, admin.getSubject());
    }

    public Map<String, Object> studentKnowledgeGraph(Integer studentId, String subject) {
        Map<String, Object> student = loadStudentProfile(studentId, subject);
        if (student == null) {
            throw new CustomException("学生不存在或不属于当前学科");
        }

        List<Map<String, Object>> points = jdbcTemplate.queryForList(
                "select kp_name, description, chapter_refs, exercise_count from knowledge_point where subject=? order by exercise_count desc, kp_name asc",
                subject
        );
        List<Map<String, Object>> relations = jdbcTemplate.queryForList(
                "select id, source_kp, target_kp, relation_type, weight from knowledge_relation where subject=? order by source_kp, target_kp",
                subject
        );
        Map<String, KnowledgeMetric> metrics = loadStudentMetrics(studentId, subject);

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> point : points) {
            String kpName = Objects.toString(point.get("kp_name"), "");
            KnowledgeMetric metric = metrics.getOrDefault(kpName, KnowledgeMetric.empty());
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", kpName);
            node.put("label", kpName);
            node.put("description", point.get("description"));
            node.put("exerciseCount", ((Number) point.get("exercise_count")).intValue());
            node.put("chapters", parseStringList((String) point.get("chapter_refs")));
            node.put("wrongTimes", metric.wrongTimes());
            node.put("totalTimes", metric.totalTimes());
            node.put("exposure", metric.exposure());
            node.put("mastery", metric.mastery());
            node.put("weakness", metric.weakness());
            node.put("status", metric.status());
            nodes.add(node);
        }

        List<Map<String, Object>> edges = relations.stream().map(edge -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", edge.get("id"));
            item.put("source", edge.get("source_kp"));
            item.put("target", edge.get("target_kp"));
            item.put("relationType", edge.get("relation_type"));
            item.put("weight", edge.get("weight"));
            return item;
        }).collect(Collectors.toList());

        List<Map<String, Object>> weakTop = nodes.stream()
                .sorted((a, b) -> Double.compare(((Number) b.get("weakness")).doubleValue(), ((Number) a.get("weakness")).doubleValue()))
                .filter(node -> ((Number) node.get("weakness")).doubleValue() > 0)
                .limit(8)
                .collect(Collectors.toList());
        List<Map<String, Object>> masteryTop = nodes.stream()
                .sorted((a, b) -> Double.compare(((Number) b.get("mastery")).doubleValue(), ((Number) a.get("mastery")).doubleValue()))
                .filter(node -> ((Number) node.get("totalTimes")).intValue() > 0)
                .limit(8)
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("studentId", studentId);
        summary.put("studentName", student.get("name"));
        summary.put("username", student.get("username"));
        summary.put("subject", subject);
        summary.put("nodeCount", nodes.size());
        summary.put("edgeCount", edges.size());
        summary.put("activatedNodeCount", nodes.stream().filter(node -> ((Number) node.get("totalTimes")).intValue() > 0).count());
        summary.put("weakNodeCount", nodes.stream().filter(node -> "WEAK".equals(node.get("status"))).count());
        summary.put("masteredNodeCount", nodes.stream().filter(node -> "MASTERED".equals(node.get("status"))).count());
        summary.put("avgMastery", round(nodes.stream().mapToDouble(node -> ((Number) node.get("mastery")).doubleValue()).average().orElse(0)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("student", student);
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("weakTop", weakTop);
        result.put("masteryTop", masteryTop);
        return result;
    }

    public List<Map<String, Object>> relationList() {
        Account admin = requireAdmin();
        refreshKnowledgePoints(admin.getSubject());
        return jdbcTemplate.queryForList(
                "select id, subject, source_kp, target_kp, relation_type, weight from knowledge_relation where subject=? order by source_kp, target_kp, relation_type",
                admin.getSubject()
        );
    }

    public List<Map<String, Object>> pointList() {
        Account admin = requireAdmin();
        refreshKnowledgePoints(admin.getSubject());
        return jdbcTemplate.queryForList(
                "select kp_name, description, chapter_refs, exercise_count, source_type from knowledge_point where subject=? order by exercise_count desc, kp_name asc",
                admin.getSubject()
        ).stream().peek(row -> row.put("chapter_refs", parseStringList((String) row.get("chapter_refs")))).collect(Collectors.toList());
    }

    public void saveRelation(Map<String, Object> payload) {
        Account admin = requireAdmin();
        String source = cleanText(payload.get("sourceKp"));
        String target = cleanText(payload.get("targetKp"));
        String relationType = cleanText(payload.getOrDefault("relationType", "related"));
        double weight = parseWeight(payload.get("weight"));
        if (source.isEmpty() || target.isEmpty()) {
            throw new CustomException("请填写完整的知识点关系");
        }
        jdbcTemplate.update(
                """
                insert into knowledge_relation(subject, source_kp, target_kp, relation_type, weight)
                values(?,?,?,?,?)
                on duplicate key update weight=values(weight)
                """,
                admin.getSubject(), source, target, relationType, weight
        );
        refreshKnowledgePoints(admin.getSubject());
    }

    public void updateRelation(Long id, Map<String, Object> payload) {
        Account admin = requireAdmin();
        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from knowledge_relation where id=? and subject=?",
                Integer.class,
                id,
                admin.getSubject()
        );
        if (exists == null || exists == 0) {
            throw new CustomException("关系不存在");
        }
        String source = cleanText(payload.get("sourceKp"));
        String target = cleanText(payload.get("targetKp"));
        String relationType = cleanText(payload.getOrDefault("relationType", "related"));
        double weight = parseWeight(payload.get("weight"));
        if (source.isEmpty() || target.isEmpty()) {
            throw new CustomException("请填写完整的知识点关系");
        }
        jdbcTemplate.update(
                "update knowledge_relation set source_kp=?, target_kp=?, relation_type=?, weight=? where id=? and subject=?",
                source, target, relationType, weight, id, admin.getSubject()
        );
        refreshKnowledgePoints(admin.getSubject());
    }

    public void deleteRelation(Long id) {
        Account admin = requireAdmin();
        jdbcTemplate.update("delete from knowledge_relation where id=? and subject=?", id, admin.getSubject());
        refreshKnowledgePoints(admin.getSubject());
    }

    private Map<String, Object> loadStudentProfile(Integer studentId, String subject) {
        try {
            return jdbcTemplate.queryForMap(
                    "select id, username, name, avatar, subject from sys_user where id=? and role='STUDENT' and subject=?",
                    studentId,
                    subject
            );
        } catch (DataAccessException e) {
            return null;
        }
    }

    private Map<String, KnowledgeMetric> loadStudentMetrics(Integer studentId, String subject) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                select e.knowledge_points,
                       sum(case when ua.is_correct=0 then 1 else 0 end) as wrong_times,
                       count(*) as total_times
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=?
                group by e.knowledge_points
                """,
                studentId,
                subject
        );
        List<String> exposures = jdbcTemplate.queryForList(
                """
                select e.knowledge_points
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=?
                """,
                String.class,
                studentId,
                subject
        );

        Map<String, Integer> exposureByKp = new HashMap<>();
        for (String kpJson : exposures) {
            for (String kp : parseKnowledgePoints(kpJson)) {
                exposureByKp.merge(kp, 1, Integer::sum);
            }
        }

        Map<String, int[]> stats = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int wrong = ((Number) row.get("wrong_times")).intValue();
            int total = ((Number) row.get("total_times")).intValue();
            for (String kp : parseKnowledgePoints((String) row.get("knowledge_points"))) {
                int[] slot = stats.computeIfAbsent(kp, key -> new int[2]);
                slot[0] += wrong;
                slot[1] += total;
            }
        }

        Map<String, KnowledgeMetric> metrics = new HashMap<>();
        for (Map.Entry<String, int[]> entry : stats.entrySet()) {
            int wrong = entry.getValue()[0];
            int total = entry.getValue()[1];
            int exposure = exposureByKp.getOrDefault(entry.getKey(), 0);
            metrics.put(entry.getKey(), KnowledgeMetric.of(wrong, total, exposure));
        }
        for (Map.Entry<String, Integer> entry : exposureByKp.entrySet()) {
            metrics.putIfAbsent(entry.getKey(), KnowledgeMetric.of(0, 0, entry.getValue()));
        }
        return metrics;
    }

    private List<String> parseKnowledgePoints(String kpJson) {
        if (ObjectUtil.isEmpty(kpJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(kpJson, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (ObjectUtil.isEmpty(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String writeJson(List<String> data) {
        try {
            return objectMapper.writeValueAsString(data.stream().filter(item -> item != null && !item.isBlank()).distinct().toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    private String cleanText(Object raw) {
        return Objects.toString(raw, "").trim();
    }

    private double parseWeight(Object raw) {
        try {
            double value = Double.parseDouble(Objects.toString(raw, "1.0"));
            return Math.max(0.1, Math.min(5.0, value));
        } catch (Exception e) {
            return 1.0;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Account requireLogin() {
        Account current = UserContext.get();
        if (current == null) {
            throw new CustomException("401");
        }
        return current;
    }

    private Account requireAdmin() {
        Account current = requireLogin();
        if (!"ADMIN".equals(current.getRole())) {
            throw new CustomException("仅管理员可操作");
        }
        return current;
    }

    private record KnowledgeMetric(int wrongTimes, int totalTimes, int exposure, double mastery, double weakness, String status) {
        private static KnowledgeMetric of(int wrongTimes, int totalTimes, int exposure) {
            double wrongRate = totalTimes == 0 ? 0 : (double) wrongTimes / totalTimes;
            double weakness = totalTimes == 0 ? 0 : Math.min(1.0, wrongRate * (0.7 + Math.log1p(totalTimes) / 2));
            double mastery = totalTimes == 0 ? 0.15 : Math.max(0, Math.min(1.0, 1 - wrongRate));
            String status;
            if (totalTimes == 0 && exposure > 0) {
                status = "SEEN";
            } else if (weakness >= 0.45) {
                status = "WEAK";
            } else if (mastery >= 0.8 && totalTimes >= 2) {
                status = "MASTERED";
            } else if (totalTimes > 0) {
                status = "LEARNING";
            } else {
                status = "UNSEEN";
            }
            return new KnowledgeMetric(wrongTimes, totalTimes, exposure, mastery, weakness, status);
        }

        private static KnowledgeMetric empty() {
            return new KnowledgeMetric(0, 0, 0, 0.15, 0, "UNSEEN");
        }
    }
}
