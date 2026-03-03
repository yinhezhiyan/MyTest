package com.example.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        initUserTable();
        migrateLegacyExerciseTableIfNeeded();
        initExerciseTable();
        initUserAnswerTable();

        initAdmin("DS");
        initAdmin("OS");
        initAdmin("CN");
        initAdmin("CO");

        importDefaultBankIfNeeded("DS", "ds.json");
        importDefaultBankIfNeeded("OS", "os.json");
        importDefaultBankIfNeeded("CN", "cn.json");
        importDefaultBankIfNeeded("CO", "co.json");
    }

    private void initUserTable() {
        jdbcTemplate.execute("""
                create table if not exists sys_user (
                    id int primary key auto_increment,
                    username varchar(50) not null,
                    password varchar(100) not null,
                    name varchar(100),
                    avatar varchar(255),
                    role varchar(20) not null,
                    subject varchar(20) not null,
                    status tinyint default 1,
                    created_at datetime default current_timestamp,
                    unique key uk_user_identity (username, role, subject)
                )
                """);
    }

    private void migrateLegacyExerciseTableIfNeeded() {
        if (!tableExists("exercise")) {
            return;
        }
        boolean isLegacy = columnExists("exercise", "title") && columnExists("exercise", "content") && !columnExists("exercise", "stem");
        if (!isLegacy) {
            return;
        }

        String backupName = "exercise_legacy_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        jdbcTemplate.execute("rename table exercise to " + backupName);
    }

    private void initExerciseTable() {
        jdbcTemplate.execute("""
                create table if not exists exercise (
                    id varchar(64) primary key,
                    subject varchar(20) not null,
                    chapter varchar(100) not null,
                    chapter_slug varchar(100),
                    stem text not null,
                    option_a varchar(255),
                    option_b varchar(255),
                    option_c varchar(255),
                    option_d varchar(255),
                    answer varchar(4) not null,
                    analysis text,
                    difficulty int default 2,
                    knowledge_points text,
                    created_at datetime default current_timestamp,
                    index idx_exercise_subject(subject)
                )
                """);
    }

    private void initUserAnswerTable() {
        jdbcTemplate.execute("""
                create table if not exists user_answer (
                    id bigint primary key auto_increment,
                    user_id int not null,
                    subject varchar(20) not null,
                    exercise_id varchar(64) not null,
                    is_correct tinyint not null,
                    chosen_option varchar(4),
                    correct_answer varchar(4),
                    answered_at datetime,
                    index idx_answer_user_subject(user_id, subject),
                    index idx_answer_exercise(exercise_id)
                )
                """);
    }

    private void initAdmin(String subject) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from sys_user where username = 'admin' and role = 'ADMIN' and subject = ?",
                Integer.class,
                subject
        );
        if (count != null && count == 0) {
            jdbcTemplate.update(
                    "insert into sys_user(username, password, name, role, subject) values(?, ?, ?, 'ADMIN', ?)",
                    "admin",
                    passwordEncoder.encode("admin"),
                    "管理员",
                    subject
            );
        }
    }

    private void importDefaultBankIfNeeded(String subject, String fileName) {
        Integer count = jdbcTemplate.queryForObject("select count(1) from exercise where subject = ?", Integer.class, subject);
        if (count != null && count > 0) {
            return;
        }
        Path file = resolveQuestionBank(fileName);
        if (file == null) {
            return;
        }
        try {
            String content = Files.readString(file);
            List<Map<String, Object>> items = objectMapper.readValue(content, new TypeReference<>() {});
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
            }
        } catch (Exception ignored) {
        }
    }

    private Path resolveQuestionBank(String fileName) {
        List<Path> candidates = List.of(
                Path.of("data/question-bank", fileName),
                Path.of("../data/question-bank", fileName),
                Path.of("/workspace/MyTest/data/question-bank", fileName)
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(String tableName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                tableName
        );
        return cnt != null && cnt > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer cnt = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.columns where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        return cnt != null && cnt > 0;
    }
}
