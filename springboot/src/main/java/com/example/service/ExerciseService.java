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
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ExerciseService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
        try { e.setAttachmentUrl(rs.getString("attachment_url")); } catch (Exception ignored) {}
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
            jdbcTemplate.update("delete from exercise where subject=?", subject);
            int inserted = 0;
            int updated = 0;
            for (Map<String, Object> item : items) {
                String id = String.valueOf(item.get("id"));
                Map<String, String> options = (Map<String, String>) item.getOrDefault("options", Map.of());
                List<String> kps = (List<String>) item.getOrDefault("knowledge_points", new ArrayList<>());
                String kp = objectMapper.writeValueAsString(kps);
                jdbcTemplate.update("""
                        insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points, attachment_url)
                        values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                        id, subject, String.valueOf(item.get("chapter")), String.valueOf(item.get("chapterSlug")),
                        String.valueOf(item.get("stem")), options.get("A"), options.get("B"), options.get("C"), options.get("D"),
                        String.valueOf(item.get("answer")), String.valueOf(item.getOrDefault("analysis", "")),
                        Integer.parseInt(String.valueOf(item.getOrDefault("difficulty", 2))), kp, String.valueOf(item.getOrDefault("attachmentUrl", "")));
                inserted++;
            }
            Integer total = jdbcTemplate.queryForObject("select count(1) from exercise where subject=?", Integer.class, subject);
            Map<String, Object> summary = new HashMap<>();
            summary.put("subject", subject);
            summary.put("inserted", inserted);
            summary.put("updated", updated);
            summary.put("processed", inserted);
            summary.put("total", total == null ? 0 : total);
            return summary;
        } catch (Exception e) {
            throw new CustomException("导入失败:" + e.getMessage());
        }
    }

    public Exercise randomExercise() {
        String subject = requireLogin().getSubject();
        List<Exercise> list = jdbcTemplate.query("select * from exercise where subject=? order by rand() limit 1", exerciseMapper, subject);
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
        List<Exercise> all = jdbcTemplate.query("select * from exercise where subject=?", exerciseMapper, subject);
        Set<String> done = new HashSet<>(jdbcTemplate.queryForList("select distinct exercise_id from user_answer where user_id=? and subject=?", String.class, uid, subject));
        Map<String, Double> cfScores = computeCollaborativeFilteringScores(subject, uid);
        Map<String, Double> weakKnowledge = loadWeakKnowledge(subject, uid);
        Map<String, List<KnowledgeEdge>> relationGraph = loadKnowledgeRelationGraph(subject);

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Exercise e : all) {
            if (!includeDone && done.contains(e.getId())) {
                continue;
            }
            double cfScore = cfScores.getOrDefault(e.getId(), 0.0);
            double kgScore = knowledgeGraphScore(e, weakKnowledge, relationGraph);
            double finalScore = 0.6 * cfScore + 0.4 * kgScore;
            ranked.add(buildRecommendationItem(e, finalScore, ""));
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

    private Map<String, Double> computeCollaborativeFilteringScores(String subject, Integer currentUserId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select user_id, exercise_id,
                       avg(case when is_correct=1 then 0.2 else 1.0 end) as preference
                from user_answer
                where subject=?
                group by user_id, exercise_id
                """, subject);

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
            double similarity = cosineSimilarity(currentUserPrefs, entry.getValue());
            if (similarity > 0) {
                similarityByUser.put(userId, similarity);
            }
        }

        Map<String, Double> scores = new HashMap<>();
        for (Map.Entry<Integer, Double> sim : similarityByUser.entrySet()) {
            Map<String, Double> otherPrefs = userItemPrefs.getOrDefault(sim.getKey(), Map.of());
            for (Map.Entry<String, Double> pref : otherPrefs.entrySet()) {
                if (currentUserPrefs.containsKey(pref.getKey())) continue;
                scores.merge(pref.getKey(), sim.getValue() * pref.getValue(), Double::sum);
            }
        }
        return scores;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (Map.Entry<String, Double> e : a.entrySet()) {
            double av = e.getValue();
            normA += av * av;
            double bv = b.getOrDefault(e.getKey(), 0.0);
            dot += av * bv;
        }
        for (double bv : b.values()) {
            normB += bv * bv;
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<String, Double> loadWeakKnowledge(String subject, Integer userId) {
        List<String> wrongKnowledgeJson = jdbcTemplate.queryForList("""
                select e.knowledge_points
                from user_answer ua
                join exercise e on ua.exercise_id = e.id and ua.subject = e.subject
                where ua.user_id=? and ua.subject=? and ua.is_correct=0
                """, String.class, userId, subject);
        Map<String, Double> weakKnowledge = new HashMap<>();
        for (String kpJson : wrongKnowledgeJson) {
            for (String kp : parseKnowledgePoints(kpJson)) {
                weakKnowledge.merge(kp, 1.0, Double::sum);
            }
        }
        return weakKnowledge;
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
                    relationScore += weak.getValue() * edge.weight();
                }
            }
        }
        return directScore + relationScore;
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

    private record KnowledgeEdge(String target, String relationType, double weight) {}

    public List<Map<String, Object>> dailyUnseen(int topN) {
        return recommendations(topN, false);
    }

    private Map<String, Object> buildRecommendationItem(Exercise e, double score, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("exerciseId", e.getId());
        item.put("chapter", e.getChapter());
        item.put("stem", e.getStem());
        item.put("score", score);
        return item;
    }



    public List<Map<String, Object>> chapterBank() {
        Account current = requireAdmin();
        List<Map<String, Object>> chapters = jdbcTemplate.queryForList("select chapter, count(1) as total from exercise where subject=? group by chapter order by chapter", current.getSubject());
        for (Map<String, Object> c : chapters) {
            List<Map<String, Object>> exercises = jdbcTemplate.queryForList("select id, stem, option_a, option_b, option_c, option_d, answer, attachment_url from exercise where subject=? and chapter=? order by id", current.getSubject(), c.get("chapter"));
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
                insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points, attachment_url)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                id, current.getSubject(), e.getChapter(), slug(e.getChapter()), e.getStem(), e.getOptionA(), e.getOptionB(), e.getOptionC(), e.getOptionD(),
                e.getAnswer(), ObjectUtil.defaultIfNull(e.getAnalysis(), ""), ObjectUtil.defaultIfNull(e.getDifficulty(), 2), ObjectUtil.defaultIfNull(e.getKnowledgePoints(), "[]"), ObjectUtil.defaultIfNull(e.getAttachmentUrl(), ""));
    }

    public void deleteExerciseByAdmin(String id) {
        Account current = requireAdmin();
        Integer cnt = jdbcTemplate.queryForObject("select count(1) from exercise where id=? and subject=?", Integer.class, id, current.getSubject());
        if (cnt == null || cnt == 0) throw new CustomException("题目不存在或无权限");
        jdbcTemplate.update("delete from exercise where id=? and subject=?", id, current.getSubject());
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
}
