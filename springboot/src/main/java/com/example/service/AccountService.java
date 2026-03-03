package com.example.service;

import com.example.context.UserContext;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @Resource
    private AdminMapper adminMapper;

    public Account profile() {
        Account current = UserContext.get();
        if (current == null) throw new CustomException("401");
        Admin db = adminMapper.selectByIdentity(current.getUsername(), current.getRole(), current.getSubject());
        if (db == null) throw new CustomException("用户不存在");
        db.setPassword(null);
        return db;
    }

    public Account updateProfile(Account payload) {
        Account current = UserContext.get();
        if (current == null) throw new CustomException("401");
        Admin db = adminMapper.selectByIdentity(current.getUsername(), current.getRole(), current.getSubject());
        if (db == null) throw new CustomException("用户不存在");
        db.setName(payload.getName());
        db.setAvatar(payload.getAvatar());
        adminMapper.updateById(db);
        db.setPassword(null);
        return db;
    }
}
