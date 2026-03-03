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

        initAdmin("MATH");
        initAdmin("OS");
        initAdmin("DS");
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
