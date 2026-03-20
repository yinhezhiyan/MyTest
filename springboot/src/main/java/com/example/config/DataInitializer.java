package com.example.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.service.KnowledgeGraphService;
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

    private static final String BANK_TYPE_MAIN = "MAIN";
    private static final String BANK_TYPE_EXTENSION = "EXTENSION";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeGraphService knowledgeGraphService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, KnowledgeGraphService knowledgeGraphService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @Override
    public void run(String... args) {
        initUserTable();
        migrateLegacyExerciseTableIfNeeded();
        initExerciseTable();
        ensureExerciseColumns();
        initUserAnswerTable();
        initKnowledgePointTable();
        initKnowledgeRelationTable();

        initAdmin("DS");
        initAdmin("OS");
        initAdmin("CN");
        initAdmin("CO");

        ensureSubjectBankFromFile("DS", "ds.json", BANK_TYPE_MAIN);
        ensureSubjectBankFromFile("OS", "os.json", BANK_TYPE_MAIN);
        ensureSubjectBankFromFile("CN", "cn.json", BANK_TYPE_MAIN);
        ensureSubjectBankFromFile("CO", "co.json", BANK_TYPE_MAIN);

        ensureSubjectBankFromFile("DS", "ds-extension.json", BANK_TYPE_EXTENSION);
        ensureSubjectBankFromFile("OS", "os-extension.json", BANK_TYPE_EXTENSION);
        ensureSubjectBankFromFile("CN", "cn-extension.json", BANK_TYPE_EXTENSION);
        ensureSubjectBankFromFile("CO", "co-extension.json", BANK_TYPE_EXTENSION);

        seedKnowledgeRelations();
        knowledgeGraphService.refreshAllSubjects(List.of("DS", "OS", "CN", "CO"));
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
                    attachment_url varchar(255),
                    bank_type varchar(20) default 'MAIN',
                    created_at datetime default current_timestamp,
                    index idx_exercise_subject(subject),
                    index idx_exercise_subject_bank(subject, bank_type)
                )
                """);
    }

    private void ensureExerciseColumns() {
        if (!columnExists("exercise", "attachment_url")) {
            jdbcTemplate.execute("alter table exercise add column attachment_url varchar(255)");
        }
        if (!columnExists("exercise", "bank_type")) {
            jdbcTemplate.execute("alter table exercise add column bank_type varchar(20) default 'MAIN'");
        }
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

    private void initKnowledgePointTable() {
        jdbcTemplate.execute("""
                create table if not exists knowledge_point (
                    id bigint primary key auto_increment,
                    subject varchar(20) not null,
                    kp_name varchar(100) not null,
                    description text,
                    chapter_refs text,
                    exercise_count int default 0,
                    source_type varchar(20) default 'AUTO',
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    unique key uk_kp_subject_name(subject, kp_name),
                    index idx_kp_subject(subject)
                )
                """);
    }

    private void initKnowledgeRelationTable() {
        jdbcTemplate.execute("""
                create table if not exists knowledge_relation (
                    id bigint primary key auto_increment,
                    subject varchar(20) not null,
                    source_kp varchar(100) not null,
                    target_kp varchar(100) not null,
                    relation_type varchar(32) default 'related',
                    weight decimal(6,2) default 1.00,
                    created_at datetime default current_timestamp,
                    updated_at datetime default current_timestamp on update current_timestamp,
                    unique key uk_kg_subject_edge(subject, source_kp, target_kp, relation_type),
                    index idx_kg_subject_source(subject, source_kp),
                    index idx_kg_subject_target(subject, target_kp)
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

    private void ensureSubjectBankFromFile(String subject, String fileName, String bankType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from exercise where subject=? and bank_type=?",
                Integer.class,
                subject,
                bankType
        );
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
                Map<String, String> options = (Map<String, String>) item.getOrDefault("options", Map.of());
                List<String> kps = (List<String>) item.getOrDefault("knowledge_points", new ArrayList<>());
                String kp = objectMapper.writeValueAsString(kps);
                jdbcTemplate.update("""
                        insert into exercise(id, subject, chapter, chapter_slug, stem, option_a, option_b, option_c, option_d, answer, analysis, difficulty, knowledge_points, attachment_url, bank_type)
                        values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                        String.valueOf(item.get("id")), subject, String.valueOf(item.get("chapter")), String.valueOf(item.get("chapterSlug")),
                        String.valueOf(item.get("stem")), options.get("A"), options.get("B"), options.get("C"), options.get("D"),
                        String.valueOf(item.get("answer")), String.valueOf(item.getOrDefault("analysis", "")),
                        Integer.parseInt(String.valueOf(item.getOrDefault("difficulty", 2))), kp,
                        String.valueOf(item.getOrDefault("attachment_url", "")), bankType);
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化题库失败: " + subject + "(" + fileName + ")", e);
        }
    }

    private void seedKnowledgeRelations() {
        List<Object[]> rows = List.of(
                new Object[]{"DS", "数据结构的基本概念", "算法复杂度", "related", 1.15},
                new Object[]{"DS", "线性表", "递归与分治", "related", 1.05},
                new Object[]{"DS", "树和二叉树", "平衡树与索引", "contains", 1.20},
                new Object[]{"DS", "图", "最短路径与导航", "contains", 1.20},
                new Object[]{"OS", "操作系统的基本概念", "系统调用与用户态内核态", "contains", 1.15},
                new Object[]{"OS", "进程管理", "容器与命名空间", "related", 1.20},
                new Object[]{"OS", "存储管理", "虚拟化与内存隔离", "related", 1.10},
                new Object[]{"OS", "文件管理", "日志型文件系统", "contains", 1.05},
                new Object[]{"CN", "计算机网络概述", "QUIC与HTTP/3", "related", 1.20},
                new Object[]{"CN", "数据链路层", "交换网络与VLAN", "contains", 1.10},
                new Object[]{"CN", "网络层", "软件定义网络", "related", 1.15},
                new Object[]{"CN", "运输层", "拥塞控制演进", "contains", 1.10},
                new Object[]{"CO", "计算机系统层次结构", "性能评测与Amdahl定律", "related", 1.15},
                new Object[]{"CO", "运算方法和运算器", "SIMD与并行计算", "related", 1.10},
                new Object[]{"CO", "存储系统", "多级缓存一致性", "contains", 1.20},
                new Object[]{"CO", "中央处理器", "指令流水线与冒险处理", "contains", 1.15}
        );
        for (Object[] row : rows) {
            jdbcTemplate.update(
                    """
                    insert into knowledge_relation(subject, source_kp, target_kp, relation_type, weight)
                    values(?,?,?,?,?)
                    on duplicate key update weight=values(weight)
                    """,
                    row
            );
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
