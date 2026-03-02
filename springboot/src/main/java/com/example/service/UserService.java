package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    public User register(User user) {
        if (ObjectUtil.isNotNull(userMapper.selectByUsername(user.getUsername()))) {
            throw new CustomException("用户名已存在");
        }
        user.setRole("STUDENT");
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User dbUser = userMapper.selectByUsername(username);
        if (ObjectUtil.isNull(dbUser) || !password.equals(dbUser.getPassword())) {
            throw new CustomException("账号或密码错误");
        }
        return dbUser;
    }

    public void add(User user) { register(user); }
    public void updateById(User user) { userMapper.updateById(user); }
    public void deleteById(Integer id) { userMapper.deleteById(id); }
    public User selectById(Integer id) { return userMapper.selectById(id); }
    public List<User> selectAll(User user) { return userMapper.selectAll(user); }

    public PageInfo<User> selectPage(User user, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return PageInfo.of(userMapper.selectAll(user));
    }
}
