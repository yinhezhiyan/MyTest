package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.context.UserContext;
import com.example.dto.SubmitAnswerRequest;
import com.example.entity.Account;
import com.example.entity.Exercise;
import com.example.exception.CustomException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return e;
    };

    public int importFromJson(String subject, String filePath) {
        Account current = requireLogin();
        if (!"ADMIN".equals(current.getRole())) throw new CustomException("仅管理员可导入");
        if (!current.getSubject().equals(subject)) throw new CustomException("不可跨学科导入");
        try {
            Path source = resolveQuestionBankPath(subject, filePath);
            String content = Files.readString(source);
            List<Map<String, Object>> items = objectMapper.readValue(content, new TypeReference<>() {});
            int count = 0;
            for (Map<String, Object> item : items) {
                Map<String, String> options = (Map<String, String>) item.get("options");
                List<String> kps = (List<String>) item.getOrDefault("knowledge_points", new ArrayList<>());
                String kp = objectMapper.writeValueAsString(kps);
                jdbcTemplate.update("""
                        insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points)
                        values(?,?,?,?,?,?,?,?,?,?,?,?,?)
                        on duplicate key update chapter=values(chapter), chapter_slug=values(chapter_slug), stem=values(stem),
                        option_a=values(option_a), option_b=values(option_b), option_c=values(option_c), option_d=values(option_d),
                        answer=values(answer), analysis=values(analysis), difficulty=values(difficulty), knowledge_points=values(knowledge_points)
                        """,
                        String.valueOf(item.get("id")), subject, String.valueOf(item.get("chapter")), String.valueOf(item.get("chapterSlug")),
                        String.valueOf(item.get("stem")), options.get("A"), options.get("B"), options.get("C"), options.get("D"),
                        String.valueOf(item.get("answer")), String.valueOf(item.getOrDefault("analysis", "")),
                        Integer.parseInt(String.valueOf(item.getOrDefault("difficulty", 2))), kp);
                count++;
            }
            return count;
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
        List<Map<String, Object>> stats = jdbcTemplate.queryForList("""
                select exercise_id, sum(case when is_correct=0 then 2 else 0 end) + count(*) * 0.5 as score
                from user_answer where user_id=? and subject=?
                group by exercise_id
                """, uid, subject);

        Map<String, Double> cf = new HashMap<>();
        for (Map<String, Object> st : stats) {
            cf.put((String) st.get("exercise_id"), ((Number) st.get("score")).doubleValue());
        }

        Set<String> weakChapters = jdbcTemplate.queryForList("""
                select e.chapter from user_answer ua
                join exercise e on ua.exercise_id=e.id
                where ua.user_id=? and ua.subject=? and ua.is_correct=0
                group by e.chapter
                """, String.class, uid, subject).stream().collect(Collectors.toSet());

        List<Exercise> all = jdbcTemplate.query("select * from exercise where subject=?", exerciseMapper, subject);
        Set<String> done = new HashSet<>(jdbcTemplate.queryForList("select distinct exercise_id from user_answer where user_id=? and subject=?", String.class, uid, subject));

        List<Map<String, Object>> ranked = new ArrayList<>();
        for (Exercise e : all) {
            if (!includeDone && done.contains(e.getId())) {
                continue;
            }
            double cfScore = weakChapters.contains(e.getChapter()) ? 1.0 : 0.3;
            cfScore += cf.getOrDefault(e.getId(), 0.0);
            double kgScore = weakChapters.contains(e.getChapter()) ? 1.0 : 0.2;
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
}
