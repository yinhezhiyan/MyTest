package com.example.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
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

        initAdmin("DS");
        initAdmin("OS");
        initAdmin("CN");
        initAdmin("CO");
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
}
