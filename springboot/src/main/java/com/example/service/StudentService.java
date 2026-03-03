package com.example.service;

import com.example.context.UserContext;
import com.example.entity.Account;
import com.example.exception.CustomException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentService {

    private final JdbcTemplate jdbcTemplate;

    public StudentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> selectPage(Integer pageNum, Integer pageSize, String name) {
        Account current = requireAdmin();
        int offset = (pageNum - 1) * pageSize;
        String where = " where role='STUDENT' and subject=? ";
        Object[] countArgs;
        Object[] listArgs;
        if (name != null && !name.isBlank()) {
            where += " and name like concat('%', ?, '%') ";
            countArgs = new Object[]{current.getSubject(), name};
            listArgs = new Object[]{current.getSubject(), name, offset, pageSize};
        } else {
            countArgs = new Object[]{current.getSubject()};
            listArgs = new Object[]{current.getSubject(), offset, pageSize};
        }
        Integer total = jdbcTemplate.queryForObject("select count(1) from sys_user" + where, Integer.class, countArgs);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "select id,username,name,avatar,subject from sys_user" + where + " order by id desc limit ?, ?", listArgs);
        Map<String, Object> page = new HashMap<>();
        page.put("list", list);
        page.put("total", total == null ? 0 : total);
        return page;
    }

    public List<Map<String, Object>> records(Integer studentId, String date) {
        Account current = requireAdmin();
        Integer exists = jdbcTemplate.queryForObject(
                "select count(1) from sys_user where id=? and role='STUDENT' and subject=?",
                Integer.class, studentId, current.getSubject());
        if (exists == null || exists == 0) throw new CustomException("学生不存在或不属于当前学科");
        StringBuilder sql = new StringBuilder("""
                select ua.exercise_id, ua.answered_at, ua.is_correct, ua.chosen_option, ua.correct_answer, e.chapter, e.stem
                from user_answer ua join exercise e on ua.exercise_id=e.id
                where ua.user_id=? and ua.subject=?
                """);
        java.util.List<Object> args = new java.util.ArrayList<>(java.util.List.of(studentId, current.getSubject()));
        if (date != null && !date.isBlank()) {
            sql.append(" and date(ua.answered_at)=?");
            args.add(date);
        }
        sql.append(" order by ua.answered_at desc");
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    private Account requireAdmin() {
        Account current = UserContext.get();
        if (current == null) throw new CustomException("401");
        if (!"ADMIN".equals(current.getRole())) throw new CustomException("仅管理员可操作");
        return current;
    }
}
