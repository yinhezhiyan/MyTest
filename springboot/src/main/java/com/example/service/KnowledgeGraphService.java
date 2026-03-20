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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private static final Pattern INVALID_KNOWLEDGE_POINT = Pattern.compile("(?i)(question-?txt导入|txt导入|ocr导入|图片ocr导入|导入标记|导入来源)");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgeGraphService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void refreshAllSubjects(List<String> subjects) {
        for (String subject : subjects) {
            sanitizeExerciseKnowledgePoints(subject);
            normalizeKnowledgeRelations(subject);
            refreshKnowledgePoints(subject);
        }
    }

    public List<String> sanitizeKnowledgePoints(Collection<String> rawKnowledgePoints) {
        if (rawKnowledgePoints == null) {
            return List.of();
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String raw : rawKnowledgePoints) {
            String normalized = normalizeKnowledgePoint(raw);
            if (!normalized.isEmpty()) {
                cleaned.add(normalized);
            }
        }
        return new ArrayList<>(cleaned);
    }

    public String sanitizeKnowledgePointsJson(String knowledgePointsJson) {
        return writeJson(sanitizeKnowledgePoints(parseKnowledgePoints(knowledgePointsJson)));
    }

    public void sanitizeExerciseKnowledgePoints(String subject) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, knowledge_points from exercise where subject=?",
                subject
        );
        for (Map<String, Object> row : rows) {
            String exerciseId = Objects.toString(row.get("id"), "");
            String original = Objects.toString(row.get("knowledge_points"), "[]");
            String sanitized = sanitizeKnowledgePointsJson(original);
            if (!Objects.equals(original, sanitized)) {
                jdbcTemplate.update("update exercise set knowledge_points=? where id=? and subject=?", sanitized, exerciseId, subject);
            }
        }
    }

    public void normalizeKnowledgeRelations(String subject) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, source_kp, target_kp, relation_type, weight from knowledge_relation where subject=? order by id asc",
                subject
        );
        Map<String, Long> keeper = new LinkedHashMap<>();
        Set<Long> deleteIds = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String source = normalizeKnowledgePoint((String) row.get("source_kp"));
            String target = normalizeKnowledgePoint((String) row.get("target_kp"));
            String relationType = normalizeRelationType((String) row.get("relation_type"));
            double weight = parseWeight(row.get("weight"));

            if (source.isEmpty() || target.isEmpty()) {
                deleteIds.add(id);
                continue;
            }

            jdbcTemplate.update(
                    "update knowledge_relation set source_kp=?, target_kp=?, relation_type=?, weight=? where id=? and subject=?",
                    source, target, relationType, weight, id, subject
            );
            String signature = source + "|" + target + "|" + relationType;
            if (keeper.containsKey(signature)) {
                deleteIds.add(id);
            } else {
                keeper.put(signature, id);
            }
        }
        for (Long deleteId : deleteIds) {
            jdbcTemplate.update("delete from knowledge_relation where id=? and subject=?", deleteId, subject);
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
            for (String kp : sanitizeKnowledgePoints(parseKnowledgePoints((String) row.get("knowledge_points")))) {
                chapterRefs.computeIfAbsent(kp, key -> new LinkedHashSet<>()).add(chapter);
                exerciseCounts.merge(kp, 1, Integer::sum);
            }
        }

        List<Map<String, Object>> edges = jdbcTemplate.queryForList(
                "select source_kp, target_kp from knowledge_relation where subject=?",
                subject
        );
        for (Map<String, Object> edge : edges) {
            String source = normalizeKnowledgePoint((String) edge.get("source_kp"));
            String target = normalizeKnowledgePoint((String) edge.get("target_kp"));
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
                    insert into knowledge_point(subject, kp_name, chapter_refs, exercise_count, weight, source_type)
                    values(?,?,?,?,?,?)
                    on duplicate key update chapter_refs=values(chapter_refs), exercise_count=values(exercise_count)
                    """,
                    subject,
                    kp,
                    chapterJson,
                    exerciseCounts.getOrDefault(kp, 0),
                    1.0,
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

        sanitizeExerciseKnowledgePoints(subject);
        normalizeKnowledgeRelations(subject);
        refreshKnowledgePoints(subject);

        List<Map<String, Object>> points = jdbcTemplate.queryForList(
                "select id, kp_name, description, chapter_refs, exercise_count, weight from knowledge_point where subject=? order by weight desc, exercise_count desc, kp_name asc",
                subject
        );
        List<Map<String, Object>> relations = relationRows(subject);
        Map<String, KnowledgeMetric> metrics = loadStudentMetrics(studentId, subject);

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> point : points) {
            String kpName = Objects.toString(point.get("kp_name"), "");
            KnowledgeMetric metric = metrics.getOrDefault(kpName, KnowledgeMetric.empty());
            double weight = parseWeight(point.get("weight"));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", kpName);
            node.put("pointId", point.get("id"));
            node.put("label", kpName);
            node.put("description", point.get("description"));
            node.put("exerciseCount", ((Number) point.get("exercise_count")).intValue());
            node.put("weight", weight);
            node.put("chapters", parseStringList((String) point.get("chapter_refs")));
            node.put("wrongTimes", metric.wrongTimes());
            node.put("totalTimes", metric.totalTimes());
            node.put("exposure", metric.exposure());
            node.put("mastery", metric.mastery());
            node.put("weakness", metric.weakness(weight));
            node.put("status", metric.status(weight));
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
        sanitizeExerciseKnowledgePoints(admin.getSubject());
        normalizeKnowledgeRelations(admin.getSubject());
        refreshKnowledgePoints(admin.getSubject());
        return relationRows(admin.getSubject());
    }

    public List<Map<String, Object>> pointList() {
        Account admin = requireAdmin();
        sanitizeExerciseKnowledgePoints(admin.getSubject());
        normalizeKnowledgeRelations(admin.getSubject());
        refreshKnowledgePoints(admin.getSubject());
        return jdbcTemplate.queryForList(
                "select id, kp_name, description, chapter_refs, exercise_count, weight, source_type from knowledge_point where subject=? order by weight desc, exercise_count desc, kp_name asc",
                admin.getSubject()
        ).stream().peek(row -> row.put("chapter_refs", parseStringList((String) row.get("chapter_refs")))).collect(Collectors.toList());
    }

    public void updatePointWeight(Long id, Object weightRaw) {
        Account admin = requireAdmin();
        double weight = parseWeight(weightRaw);
        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from knowledge_point where id=? and subject=?",
                Integer.class,
                id,
                admin.getSubject()
        );
        if (exists == null || exists == 0) {
            throw new CustomException("知识点不存在");
        }
        jdbcTemplate.update("update knowledge_point set weight=? where id=? and subject=?", weight, id, admin.getSubject());
    }

    public void saveRelation(Map<String, Object> payload) {
        saveRelationInternal(requireAdmin().getSubject(), payload);
    }

    public void saveRelations(List<Map<String, Object>> payloads) {
        Account admin = requireAdmin();
        if (payloads == null || payloads.isEmpty()) {
            throw new CustomException("请至少提供一条知识关系");
        }
        for (Map<String, Object> payload : payloads) {
            saveRelationInternal(admin.getSubject(), payload);
        }
        normalizeKnowledgeRelations(admin.getSubject());
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
        String source = normalizeKnowledgePoint(Objects.toString(payload.get("sourceKp"), ""));
        String target = normalizeKnowledgePoint(Objects.toString(payload.get("targetKp"), ""));
        String relationType = normalizeRelationType(Objects.toString(payload.getOrDefault("relationType", "related"), "related"));
        double weight = parseWeight(payload.get("weight"));
        if (source.isEmpty() || target.isEmpty()) {
            throw new CustomException("请填写完整的知识点关系");
        }
        jdbcTemplate.update(
                "update knowledge_relation set source_kp=?, target_kp=?, relation_type=?, weight=? where id=? and subject=?",
                source, target, relationType, weight, id, admin.getSubject()
        );
        normalizeKnowledgeRelations(admin.getSubject());
        refreshKnowledgePoints(admin.getSubject());
    }

    public void deleteRelation(Long id) {
        Account admin = requireAdmin();
        jdbcTemplate.update("delete from knowledge_relation where id=? and subject=?", id, admin.getSubject());
        normalizeKnowledgeRelations(admin.getSubject());
        refreshKnowledgePoints(admin.getSubject());
    }

    private void saveRelationInternal(String subject, Map<String, Object> payload) {
        String source = normalizeKnowledgePoint(Objects.toString(payload.get("sourceKp"), ""));
        String target = normalizeKnowledgePoint(Objects.toString(payload.get("targetKp"), ""));
        String relationType = normalizeRelationType(Objects.toString(payload.getOrDefault("relationType", "related"), "related"));
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
                subject, source, target, relationType, weight
        );
    }

    private List<Map<String, Object>> relationRows(String subject) {
        return jdbcTemplate.queryForList(
                "select id, subject, source_kp, target_kp, relation_type, weight from knowledge_relation where subject=? order by source_kp, target_kp, relation_type, id",
                subject
        );
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
            for (String kp : sanitizeKnowledgePoints(parseKnowledgePoints(kpJson))) {
                exposureByKp.merge(kp, 1, Integer::sum);
            }
        }

        Map<String, int[]> stats = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int wrong = ((Number) row.get("wrong_times")).intValue();
            int total = ((Number) row.get("total_times")).intValue();
            for (String kp : sanitizeKnowledgePoints(parseKnowledgePoints((String) row.get("knowledge_points")))) {
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

    private String normalizeKnowledgePoint(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isEmpty() || INVALID_KNOWLEDGE_POINT.matcher(normalized).find()) {
            return "";
        }
        return normalized;
    }

    private String normalizeRelationType(String raw) {
        String relationType = Objects.toString(raw, "related").trim().toLowerCase(Locale.ROOT);
        return switch (relationType) {
            case "prerequisite", "contains", "related" -> relationType;
            default -> "related";
        };
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

    private record KnowledgeMetric(int wrongTimes, int totalTimes, int exposure, double mastery, double weaknessBase) {
        private static KnowledgeMetric of(int wrongTimes, int totalTimes, int exposure) {
            double wrongRate = totalTimes == 0 ? 0 : (double) wrongTimes / totalTimes;
            double weaknessBase = totalTimes == 0 ? 0 : Math.min(1.0, wrongRate * (0.7 + Math.log1p(totalTimes) / 2));
            double mastery = totalTimes == 0 ? 0.15 : Math.max(0, Math.min(1.0, 1 - wrongRate));
            return new KnowledgeMetric(wrongTimes, totalTimes, exposure, mastery, weaknessBase);
        }

        private static KnowledgeMetric empty() {
            return new KnowledgeMetric(0, 0, 0, 0.15, 0);
        }

        private double weakness(double pointWeight) {
            return Math.min(1.0, weaknessBase * Math.max(0.2, pointWeight));
        }

        private String status(double pointWeight) {
            double weakness = weakness(pointWeight);
            if (totalTimes == 0 && exposure > 0) {
                return "SEEN";
            }
            if (weakness >= 0.45) {
                return "WEAK";
            }
            if (mastery >= 0.8 && totalTimes >= 2) {
                return "MASTERED";
            }
            if (totalTimes > 0) {
                return "LEARNING";
            }
            return "UNSEEN";
        }
    }
}
