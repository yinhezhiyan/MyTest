package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.context.UserContext;
import com.example.dto.SubmitAnswerRequest;
import com.example.entity.Account;
import com.example.entity.Exercise;
import com.example.exception.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExerciseService {
    private static final String BANK_TYPE_MAIN = "MAIN";
    private static final String BANK_TYPE_EXTENSION = "EXTENSION";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeGraphService knowledgeGraphService;

    public ExerciseService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, KnowledgeGraphService knowledgeGraphService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    private final RowMapper<Exercise> exerciseMapper = (rs, rowNum) -> {
        Exercise e = new Exercise();
        e.setId(rs.getString("id"));
        e.setSubject(rs.getString("subject"));
        e.setChapter(rs.getString("chapter"));
        e.setChapterSlug(rs.getString("chapter_slug"));
        e.setStem(rs.getString("stem"));
        e.setOptionA(rs.getString("option_a"));
        e.setOptionB(rs.getString("option_b"));
        e.setOptionC(rs.getString("option_c"));
        e.setOptionD(rs.getString("option_d"));
        e.setAnswer(rs.getString("answer"));
        e.setAnalysis(rs.getString("analysis"));
        e.setDifficulty(rs.getInt("difficulty"));
        e.setKnowledgePoints(rs.getString("knowledge_points"));
        try {
            e.setAttachmentUrl(rs.getString("attachment_url"));
        } catch (Exception ignored) {
        }
        try {
            e.setBankType(rs.getString("bank_type"));
        } catch (Exception ignored) {
        }
        return e;
    };

    public Map<String, Object> importFromJson(String subject, String filePath) {
        Account current = requireLogin();
        if (!"ADMIN".equals(current.getRole())) throw new CustomException("仅管理员可导入");
        if (!current.getSubject().equals(subject)) throw new CustomException("不可跨学科导入");
        try {
            Path source = resolveQuestionBankPath(subject, filePath);
            String content = Files.readString(source);
            List<Map<String, Object>> items = objectMapper.readValue(content, new TypeReference<>() {});
            jdbcTemplate.update("delete from exercise where subject=? and bank_type=?", subject, BANK_TYPE_MAIN);
            int inserted = 0;
            int updated = 0;
            for (Map<String, Object> item : items) {
                String id = String.valueOf(item.get("id"));
                Map<String, String> options = (Map<String, String>) item.getOrDefault("options", Map.of());
                List<String> kps = (List<String>) item.getOrDefault("knowledge_points", new ArrayList<>());
                String kp = objectMapper.writeValueAsString(knowledgeGraphService.sanitizeKnowledgePoints(kps));
                jdbcTemplate.update("""
                        insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points, attachment_url, bank_type)
                        values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                        id, subject, String.valueOf(item.get("chapter")), String.valueOf(item.get("chapterSlug")),
                        String.valueOf(item.get("stem")), options.get("A"), options.get("B"), options.get("C"), options.get("D"),
                        String.valueOf(item.get("answer")), String.valueOf(item.getOrDefault("analysis", "")),
                        Integer.parseInt(String.valueOf(item.getOrDefault("difficulty", 2))), kp,
                        String.valueOf(item.getOrDefault("attachmentUrl", "")), BANK_TYPE_MAIN);
                inserted++;
            }
            Integer total = jdbcTemplate.queryForObject("select count(1) from exercise where subject=? and bank_type=?", Integer.class, subject, BANK_TYPE_MAIN);
            Map<String, Object> summary = new HashMap<>();
            summary.put("subject", subject);
            summary.put("inserted", inserted);
            summary.put("updated", updated);
            summary.put("processed", inserted);
            summary.put("total", total == null ? 0 : total);
            knowledgeGraphService.refreshKnowledgePoints(subject);
            return summary;
        } catch (Exception e) {
            throw new CustomException("导入失败:" + e.getMessage());
        }
    }

    public Exercise randomExercise() {
        String subject = requireLogin().getSubject();
        List<Exercise> list = jdbcTemplate.query("select * from exercise where subject=? and bank_type=? order by rand() limit 1", exerciseMapper, subject, BANK_TYPE_MAIN);
        if (list.isEmpty()) throw new CustomException("当前学科暂无题目");
        Exercise e = list.getFirst();
        e.setAnswer(null);
        return e;
    }

    public Exercise getById(String id) {
        String subject = requireLogin().getSubject();
        List<Exercise> list = jdbcTemplate.query("select * from exercise where id=? and subject=?", exerciseMapper, id, subject);
        if (list.isEmpty()) throw new CustomException("题目不存在");
        Exercise e = list.getFirst();
        e.setAnswer(null);
        return e;
    }

    public Map<String, Object> submit(SubmitAnswerRequest request) {
        Account user = requireLogin();
        List<Exercise> list = jdbcTemplate.query("select * from exercise where id=? and subject=?", exerciseMapper, request.getExerciseId(), user.getSubject());
        if (list.isEmpty()) throw new CustomException("题目不存在或跨学科");
        Exercise e = list.getFirst();
        boolean correct = e.getAnswer().equalsIgnoreCase(request.getChosenOption());
        jdbcTemplate.update("insert into user_answer(user_id, subject, exercise_id, is_correct, chosen_option, correct_answer, answered_at) values(?,?,?,?,?,?,?)",
                user.getId(), user.getSubject(), e.getId(), correct ? 1 : 0, request.getChosenOption(), e.getAnswer(), LocalDateTime.now());
        Map<String, Object> res = new HashMap<>();
        res.put("isCorrect", correct);
        res.put("correctAnswer", e.getAnswer());
        res.put("analysis", e.getAnalysis());
        return res;
    }

    public List<Map<String, Object>> answerRecords(String chapter, Integer correct, String date) {
        Account user = requireLogin();
        StringBuilder sql = new StringBuilder("""
                select ua.exercise_id, ua.answered_at, ua.is_correct, ua.chosen_option, ua.correct_answer, e.chapter, e.stem
                from user_answer ua join exercise e on ua.exercise_id = e.id
                where ua.user_id=? and ua.subject=?
                """);
        List<Object> args = new ArrayList<>(List.of(user.getId(), user.getSubject()));
        if (ObjectUtil.isNotEmpty(chapter)) {
            sql.append(" and e.chapter=?");
            args.add(chapter);
        }
        if (correct != null) {
            sql.append(" and ua.is_correct=?");
            args.add(correct);
        }
        if (ObjectUtil.isNotEmpty(date)) {
            sql.append(" and date(ua.answered_at)=?");
            args.add(date);
        }
        sql.append(" order by ua.answered_at desc");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    public List<Map<String, Object>> recommendations(int topN, boolean includeDone) {
        Account user = requireLogin();
        String subject = user.getSubject();
        Integer uid = user.getId();
        List<Exercise> all = loadExercises(subject, BANK_TYPE_MAIN);
        Set<String> done = loadDoneExerciseIds(uid, subject, BANK_TYPE_MAIN);
        Map<String, Double> cfScores = computeCollaborativeFilteringScores(subject, uid, BANK_TYPE_MAIN);
        Map<String, Double> weakKnowledge = loadWeakKnowledge(subject, uid, BANK_TYPE_MAIN);
        Map<String, List<KnowledgeEdge>> relationGraph = loadKnowledgeRelationGraph(subject);
        DifficultyPreference difficultyPreference = loadDifficultyPreference(subject, uid, BANK_TYPE_MAIN);
        Map<String, Integer> exposure = loadKnowledgeExposure(subject, uid, BANK_TYPE_MAIN);

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Exercise e : all) {
            if (!includeDone && done.contains(e.getId())) {
                continue;
            }
            RecommendationBreakdown breakdown = scoreMainExercise(e, cfScores, weakKnowledge, relationGraph, difficultyPreference, exposure, done);
            ranked.add(buildRecommendationItem(e, breakdown.score(), breakdown.reason(), breakdown.reasonTags()));
        }

        List<Map<String, Object>> result = ranked.stream()
                .sorted((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")))
                .limit(topN)
                .collect(Collectors.toList());
        if (result.isEmpty() && !includeDone && !done.isEmpty()) {
            return recommendations(topN, true);
        }
        return result;
    }

    private RecommendationBreakdown scoreMainExercise(Exercise exercise,
                                                      Map<String, Double> cfScores,
                                                      Map<String, Double> weakKnowledge,
                                                      Map<String, List<KnowledgeEdge>> relationGraph,
                                                      DifficultyPreference difficultyPreference,
                                                      Map<String, Integer> exposure,
                                                      Set<String> done) {
        double cfScore = normalizeScore(cfScores.getOrDefault(exercise.getId(), 0.0), 3.0);
        double weaknessScore = normalizeScore(knowledgeGraphScore(exercise, weakKnowledge, relationGraph), 6.0);
        double difficultyScore = difficultyMatch(exercise.getDifficulty(), difficultyPreference);
        double noveltyScore = done.contains(exercise.getId()) ? 0.2 : 1.0;
        double coverageScore = knowledgeCoverageScore(exercise, exposure);

        double finalScore = 0.42 * cfScore
                + 0.33 * weaknessScore
                + 0.15 * difficultyScore
                + 0.10 * coverageScore;
        finalScore *= noveltyScore;

        List<String> tags = buildMainReasonTags(cfScore, weaknessScore, difficultyScore, coverageScore, exercise);
        String reason = String.join(" · ", tags);
        return new RecommendationBreakdown(finalScore, reason, tags);
    }

    private Map<String, Double> computeCollaborativeFilteringScores(String subject, Integer currentUserId, String bankType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select ua.user_id,
                       ua.exercise_id,
                       avg(case when ua.is_correct=1 then 0.25 else 1.0 end + coalesce(e.difficulty, 2) * 0.08) as preference
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.subject=? and e.bank_type=?
                group by ua.user_id, ua.exercise_id
                """, subject, bankType);

        Map<Integer, Map<String, Double>> userItemPrefs = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            String exerciseId = (String) row.get("exercise_id");
            double preference = ((Number) row.get("preference")).doubleValue();
            userItemPrefs.computeIfAbsent(userId, k -> new HashMap<>()).put(exerciseId, preference);
        }

        Map<String, Double> currentUserPrefs = userItemPrefs.getOrDefault(currentUserId, Map.of());
        if (currentUserPrefs.isEmpty()) {
            return Map.of();
        }

        Map<Integer, Double> similarityByUser = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Double>> entry : userItemPrefs.entrySet()) {
            Integer userId = entry.getKey();
            if (Objects.equals(userId, currentUserId)) continue;
            double similarity = adjustedCosineSimilarity(currentUserPrefs, entry.getValue());
            if (similarity > 0.05) {
                int common = overlapCount(currentUserPrefs, entry.getValue());
                double shrink = common / (common + 2.0);
                similarityByUser.put(userId, similarity * shrink);
            }
        }

        Map<String, Double> weighted = new HashMap<>();
        Map<String, Double> similaritySum = new HashMap<>();
        for (Map.Entry<Integer, Double> sim : similarityByUser.entrySet()) {
            Map<String, Double> otherPrefs = userItemPrefs.getOrDefault(sim.getKey(), Map.of());
            for (Map.Entry<String, Double> pref : otherPrefs.entrySet()) {
                if (currentUserPrefs.containsKey(pref.getKey())) continue;
                weighted.merge(pref.getKey(), sim.getValue() * pref.getValue(), Double::sum);
                similaritySum.merge(pref.getKey(), sim.getValue(), Double::sum);
            }
        }

        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<String, Double> entry : weighted.entrySet()) {
            double denom = similaritySum.getOrDefault(entry.getKey(), 0.0);
            if (denom > 0) {
                scores.put(entry.getKey(), entry.getValue() / denom);
            }
        }
        return scores;
    }

    private double adjustedCosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        Set<String> common = a.keySet().stream().filter(b::containsKey).collect(Collectors.toSet());
        if (common.isEmpty()) {
            return 0;
        }
        double avgA = common.stream().mapToDouble(a::get).average().orElse(0);
        double avgB = common.stream().mapToDouble(b::get).average().orElse(0);
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (String key : common) {
            double av = a.get(key) - avgA;
            double bv = b.get(key) - avgB;
            dot += av * bv;
            normA += av * av;
            normB += bv * bv;
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int overlapCount(Map<String, Double> a, Map<String, Double> b) {
        int count = 0;
        for (String key : a.keySet()) {
            if (b.containsKey(key)) {
                count++;
            }
        }
        return count;
    }

    private Map<String, Double> loadWeakKnowledge(String subject, Integer userId, String bankType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select e.knowledge_points,
                       sum(case when ua.is_correct=0 then 1 else 0 end) as wrong_times,
                       count(*) as total_times
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=? and e.bank_type=?
                group by e.knowledge_points
                """, userId, subject, bankType);
        Map<String, Double> weakKnowledge = new HashMap<>();
        for (Map<String, Object> row : rows) {
            double wrongTimes = ((Number) row.get("wrong_times")).doubleValue();
            double totalTimes = ((Number) row.get("total_times")).doubleValue();
            double weakness = totalTimes == 0 ? 0 : (wrongTimes / totalTimes) * (1 + Math.log1p(totalTimes));
            for (String kp : parseKnowledgePoints((String) row.get("knowledge_points"))) {
                weakKnowledge.merge(kp, weakness, Double::sum);
            }
        }
        return weakKnowledge;
    }

    private Map<String, Integer> loadKnowledgeExposure(String subject, Integer userId, String bankType) {
        List<String> kpRows = jdbcTemplate.queryForList("""
                select e.knowledge_points
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=? and e.bank_type=?
                """, String.class, userId, subject, bankType);
        Map<String, Integer> exposure = new HashMap<>();
        for (String kpJson : kpRows) {
            for (String kp : parseKnowledgePoints(kpJson)) {
                exposure.merge(kp, 1, Integer::sum);
            }
        }
        return exposure;
    }

    private DifficultyPreference loadDifficultyPreference(String subject, Integer userId, String bankType) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select e.difficulty, avg(case when ua.is_correct=1 then 1 else 0 end) as accuracy, count(*) as times
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=? and e.bank_type=?
                group by e.difficulty
                """, userId, subject, bankType);
        if (rows.isEmpty()) {
            return new DifficultyPreference(2.0, 1.0);
        }
        double weightedDifficulty = 0;
        double totalWeight = 0;
        for (Map<String, Object> row : rows) {
            double difficulty = ((Number) row.get("difficulty")).doubleValue();
            double accuracy = ((Number) row.get("accuracy")).doubleValue();
            double times = ((Number) row.get("times")).doubleValue();
            double weight = times * (0.6 + (1 - Math.abs(accuracy - 0.65)));
            weightedDifficulty += difficulty * weight;
            totalWeight += weight;
        }
        double preferred = totalWeight == 0 ? 2.0 : weightedDifficulty / totalWeight;
        return new DifficultyPreference(preferred, 1.2);
    }

    private Map<String, List<KnowledgeEdge>> loadKnowledgeRelationGraph(String subject) {
        try {
            List<Map<String, Object>> edges = jdbcTemplate.queryForList("""
                    select source_kp, target_kp, relation_type, weight
                    from knowledge_relation
                    where subject=?
                    """, subject);
            Map<String, List<KnowledgeEdge>> graph = new HashMap<>();
            for (Map<String, Object> edge : edges) {
                String source = (String) edge.get("source_kp");
                String target = (String) edge.get("target_kp");
                String relationType = (String) edge.get("relation_type");
                Number weightNum = (Number) edge.get("weight");
                double weight = weightNum == null ? 1.0 : weightNum.doubleValue();
                graph.computeIfAbsent(source, k -> new ArrayList<>())
                        .add(new KnowledgeEdge(target, relationType, weight));
            }
            return graph;
        } catch (DataAccessException e) {
            return Map.of();
        }
    }

    private double knowledgeGraphScore(Exercise exercise,
                                       Map<String, Double> weakKnowledge,
                                       Map<String, List<KnowledgeEdge>> relationGraph) {
        List<String> candidateKnowledge = parseKnowledgePoints(exercise.getKnowledgePoints());
        if (candidateKnowledge.isEmpty()) {
            return 0;
        }
        Set<String> targetSet = new HashSet<>(candidateKnowledge);
        double directScore = 0;
        for (String kp : candidateKnowledge) {
            directScore += weakKnowledge.getOrDefault(kp, 0.0);
        }

        double relationScore = 0;
        for (Map.Entry<String, Double> weak : weakKnowledge.entrySet()) {
            List<KnowledgeEdge> outs = relationGraph.getOrDefault(weak.getKey(), List.of());
            for (KnowledgeEdge edge : outs) {
                if (targetSet.contains(edge.target())) {
                    double relationBoost = switch (edge.relationType()) {
                        case "prerequisite" -> 1.15;
                        case "contains" -> 1.05;
                        default -> 1.0;
                    };
                    relationScore += weak.getValue() * edge.weight() * relationBoost;
                }
            }
        }
        return directScore + relationScore;
    }

    private double knowledgeCoverageScore(Exercise exercise, Map<String, Integer> exposure) {
        List<String> knowledge = parseKnowledgePoints(exercise.getKnowledgePoints());
        if (knowledge.isEmpty()) {
            return 0.4;
        }
        double total = 0;
        for (String kp : knowledge) {
            int count = exposure.getOrDefault(kp, 0);
            total += 1.0 / (1.0 + count);
        }
        return Math.min(1.0, total / knowledge.size());
    }

    private double difficultyMatch(Integer difficulty, DifficultyPreference preference) {
        double candidate = difficulty == null ? 2.0 : difficulty;
        double diff = Math.abs(candidate - preference.preferredDifficulty());
        return Math.max(0.2, 1 - diff / Math.max(1.0, preference.tolerance()));
    }

    private List<String> buildMainReasonTags(double cfScore,
                                             double weaknessScore,
                                             double difficultyScore,
                                             double coverageScore,
                                             Exercise exercise) {
        List<String> tags = new ArrayList<>();
        if (weaknessScore >= 0.55) {
            tags.add("针对薄弱知识点强化");
        }
        if (cfScore >= 0.55) {
            tags.add("协同过滤命中相似学生错题");
        }
        if (difficultyScore >= 0.75) {
            tags.add("难度与当前水平匹配");
        }
        if (coverageScore >= 0.6) {
            tags.add("兼顾知识覆盖");
        }
        if (tags.isEmpty()) {
            tags.add("推荐复习" + exercise.getChapter());
        }
        return tags;
    }

    private double normalizeScore(double score, double ceiling) {
        if (score <= 0) {
            return 0;
        }
        return Math.min(1.0, score / ceiling);
    }

    private List<String> parseKnowledgePoints(String kpJson) {
        if (ObjectUtil.isEmpty(kpJson)) {
            return List.of();
        }
        try {
            return knowledgeGraphService.sanitizeKnowledgePoints(objectMapper.readValue(kpJson, new TypeReference<List<String>>() {}));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public List<Map<String, Object>> dailyUnseen(int topN) {
        Account user = requireLogin();
        String subject = user.getSubject();
        Integer uid = user.getId();
        List<Exercise> extensionExercises = loadExercises(subject, BANK_TYPE_EXTENSION);
        Set<String> done = loadDoneExerciseIds(uid, subject, BANK_TYPE_EXTENSION);
        Map<String, Double> weakKnowledge = loadWeakKnowledge(subject, uid, BANK_TYPE_MAIN);
        Map<String, List<KnowledgeEdge>> relationGraph = loadKnowledgeRelationGraph(subject);
        Map<String, Integer> extensionExposure = loadKnowledgeExposure(subject, uid, BANK_TYPE_EXTENSION);

        List<Map<String, Object>> result = extensionExercises.stream()
                .filter(e -> !done.contains(e.getId()))
                .map(e -> {
                    double graphScore = normalizeScore(knowledgeGraphScore(e, weakKnowledge, relationGraph), 6.0);
                    double freshnessScore = knowledgeCoverageScore(e, extensionExposure);
                    double finalScore = 0.7 * graphScore + 0.3 * freshnessScore;
                    String reason = graphScore > 0.45 ? "从你的薄弱知识点延伸拓展" : "为你补充同学科进阶知识";
                    return buildRecommendationItem(e, finalScore, reason, List.of("知识图谱拓展", reason));
                })
                .sorted((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")))
                .collect(Collectors.toList());

        if (result.size() < topN) {
            extensionExercises.stream()
                    .filter(e -> done.contains(e.getId()))
                    .map(e -> buildRecommendationItem(e, 0.28, "继续巩固本学科拓展知识", List.of("拓展知识回顾")))
                    .forEach(item -> addIfMissing(result, item, topN));
        }

        if (result.size() < topN) {
            recommendations(topN * 2, true).stream()
                    .map(item -> {
                        Map<String, Object> copy = new HashMap<>(item);
                        List<String> tags = new ArrayList<>((List<String>) copy.getOrDefault("reasonTags", List.of()));
                        tags.add(0, "每日补充练习");
                        copy.put("reasonTags", tags.stream().distinct().collect(Collectors.toList()));
                        copy.put("reason", "拓展题不足，补充推荐练习");
                        return copy;
                    })
                    .forEach(item -> addIfMissing(result, item, topN));
        }

        return result.stream().limit(topN).collect(Collectors.toList());
    }

    private void addIfMissing(List<Map<String, Object>> target, Map<String, Object> item, int limit) {
        if (target.size() >= limit) {
            return;
        }
        String exerciseId = Objects.toString(item.get("exerciseId"), "");
        boolean exists = target.stream().anyMatch(current -> Objects.equals(Objects.toString(current.get("exerciseId"), ""), exerciseId));
        if (!exists) {
            target.add(item);
        }
    }

    private Map<String, Object> buildRecommendationItem(Exercise e, double score, String reason, List<String> reasonTags) {
        Map<String, Object> item = new HashMap<>();
        item.put("exerciseId", e.getId());
        item.put("chapter", e.getChapter());
        item.put("stem", e.getStem());
        item.put("score", score);
        item.put("reason", reason);
        item.put("reasonTags", reasonTags);
        item.put("bankType", e.getBankType());
        item.put("difficulty", e.getDifficulty());
        item.put("knowledgePoints", parseKnowledgePoints(e.getKnowledgePoints()));
        return item;
    }

    public List<Map<String, Object>> chapterBank() {
        Account current = requireAdmin();
        List<Map<String, Object>> chapters = jdbcTemplate.queryForList("select chapter, count(1) as total from exercise where subject=? and bank_type=? group by chapter order by chapter", current.getSubject(), BANK_TYPE_MAIN);
        for (Map<String, Object> c : chapters) {
            List<Map<String, Object>> exercises = jdbcTemplate.queryForList("select id, stem, option_a, option_b, option_c, option_d, answer, attachment_url from exercise where subject=? and chapter=? and bank_type=? order by id", current.getSubject(), c.get("chapter"), BANK_TYPE_MAIN);
            c.put("exercises", exercises);
        }
        return chapters;
    }

    public void addExerciseByAdmin(Exercise e) {
        Account current = requireAdmin();
        if (ObjectUtil.hasEmpty(e.getChapter(), e.getStem(), e.getOptionA(), e.getOptionB(), e.getOptionC(), e.getOptionD(), e.getAnswer())) {
            throw new CustomException("题目信息不完整");
        }
        String id = (ObjectUtil.isNotEmpty(e.getId()) ? e.getId() : (slug(e.getChapter()) + "-" + System.currentTimeMillis() % 1000000));
        jdbcTemplate.update("""
                insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points, attachment_url, bank_type)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                id, current.getSubject(), e.getChapter(), slug(e.getChapter()), e.getStem(), e.getOptionA(), e.getOptionB(), e.getOptionC(), e.getOptionD(),
                e.getAnswer(), ObjectUtil.defaultIfNull(e.getAnalysis(), ""), ObjectUtil.defaultIfNull(e.getDifficulty(), 2), knowledgeGraphService.sanitizeKnowledgePointsJson(ObjectUtil.defaultIfNull(e.getKnowledgePoints(), "[]")), ObjectUtil.defaultIfNull(e.getAttachmentUrl(), ""), BANK_TYPE_MAIN);
        knowledgeGraphService.refreshKnowledgePoints(current.getSubject());
    }

    public void deleteExerciseByAdmin(String id) {
        Account current = requireAdmin();
        Integer cnt = jdbcTemplate.queryForObject("select count(1) from exercise where id=? and subject=? and bank_type=?", Integer.class, id, current.getSubject(), BANK_TYPE_MAIN);
        if (cnt == null || cnt == 0) throw new CustomException("题目不存在或无权限");
        jdbcTemplate.update("delete from exercise where id=? and subject=? and bank_type=?", id, current.getSubject(), BANK_TYPE_MAIN);
        knowledgeGraphService.refreshKnowledgePoints(current.getSubject());
    }

    public Exercise getExerciseForAdmin(String id) {
        Account current = requireAdmin();
        List<Exercise> list = jdbcTemplate.query(
                "select * from exercise where id=? and subject=? and bank_type=?",
                exerciseMapper,
                id,
                current.getSubject(),
                BANK_TYPE_MAIN
        );
        if (list.isEmpty()) {
            throw new CustomException("题目不存在或无权限");
        }
        return list.getFirst();
    }

    public void updateExerciseByAdmin(String id, Exercise e) {
        Account current = requireAdmin();
        Integer cnt = jdbcTemplate.queryForObject("select count(1) from exercise where id=? and subject=? and bank_type=?", Integer.class, id, current.getSubject(), BANK_TYPE_MAIN);
        if (cnt == null || cnt == 0) {
            throw new CustomException("题目不存在或无权限");
        }
        if (ObjectUtil.hasEmpty(e.getChapter(), e.getStem(), e.getOptionA(), e.getOptionB(), e.getOptionC(), e.getOptionD(), e.getAnswer())) {
            throw new CustomException("题目信息不完整");
        }
        jdbcTemplate.update("""
                update exercise
                set chapter=?, chapter_slug=?, stem=?, option_a=?, option_b=?, option_c=?, option_d=?,
                    answer=?, analysis=?, difficulty=?, knowledge_points=?, attachment_url=?
                where id=? and subject=? and bank_type=?
                """,
                e.getChapter(), slug(e.getChapter()), e.getStem(), e.getOptionA(), e.getOptionB(), e.getOptionC(), e.getOptionD(),
                e.getAnswer(), ObjectUtil.defaultIfNull(e.getAnalysis(), ""), ObjectUtil.defaultIfNull(e.getDifficulty(), 2),
                knowledgeGraphService.sanitizeKnowledgePointsJson(ObjectUtil.defaultIfNull(e.getKnowledgePoints(), "[]")), ObjectUtil.defaultIfNull(e.getAttachmentUrl(), ""),
                id, current.getSubject(), BANK_TYPE_MAIN);
        knowledgeGraphService.refreshKnowledgePoints(current.getSubject());
    }

    public Map<String, Object> studentHomeSummary() {
        Account user = requireLogin();
        Integer total = jdbcTemplate.queryForObject("select count(1) from user_answer where user_id=? and subject=? and date(answered_at)=curdate()", Integer.class, user.getId(), user.getSubject());
        Integer correct = jdbcTemplate.queryForObject("select count(1) from user_answer where user_id=? and subject=? and date(answered_at)=curdate() and is_correct=1", Integer.class, user.getId(), user.getSubject());
        Map<String, Object> m = new HashMap<>();
        m.put("todayTotal", total == null ? 0 : total);
        m.put("todayCorrect", correct == null ? 0 : correct);
        m.put("todayWrong", (total == null ? 0 : total) - (correct == null ? 0 : correct));
        return m;
    }

    private List<Exercise> loadExercises(String subject, String bankType) {
        return jdbcTemplate.query("select * from exercise where subject=? and bank_type=?", exerciseMapper, subject, bankType);
    }

    private Set<String> loadDoneExerciseIds(Integer userId, String subject, String bankType) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                select distinct ua.exercise_id
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=? and e.bank_type=?
                """, String.class, userId, subject, bankType));
    }

    private String slug(String text) {
        return text == null ? "chapter" : text.toLowerCase().replaceAll("[^a-z0-9\u4e00-\u9fa5]+", "-").replaceAll("-+", "-").replaceAll("(^-|-$)", "");
    }

    private Path resolveQuestionBankPath(String subject, String filePath) {
        if (ObjectUtil.isNotEmpty(filePath)) {
            Path custom = Path.of(filePath);
            if (Files.exists(custom)) return custom;
        }
        String lower = subject == null ? "" : subject.toLowerCase();
        List<Path> candidates = List.of(
                Path.of("data/question-bank", lower + ".json"),
                Path.of("../data/question-bank", lower + ".json"),
                Path.of("/workspace/MyTest/data/question-bank", lower + ".json")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) return candidate;
        }
        throw new CustomException("题库文件不存在");
    }

    private Account requireLogin() {
        Account current = UserContext.get();
        if (current == null) throw new CustomException("401");
        return current;
    }

    private Account requireAdmin() {
        Account current = requireLogin();
        if (!"ADMIN".equals(current.getRole())) throw new CustomException("仅管理员可操作");
        return current;
    }

    private record KnowledgeEdge(String target, String relationType, double weight) {}

    private record DifficultyPreference(double preferredDifficulty, double tolerance) {}

    private record RecommendationBreakdown(double score, String reason, List<String> reasonTags) {}
}
