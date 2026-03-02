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
        if (ObjectUtil.isNotNull(userMapper.selectByUsername(user.getUsername()))) throw new CustomException("用户名已存在");
        if (ObjectUtil.isEmpty(user.getSubject())) throw new CustomException("请选择学科");
        if (ObjectUtil.isNotEmpty(user.getRole()) && !"STUDENT".equals(user.getRole())) throw new CustomException("注册仅支持学生账号");
        user.setRole("STUDENT");
        userMapper.insert(user);
        return user;
    }

    public User addByAdmin(User user) {
        if (ObjectUtil.isNotNull(userMapper.selectByUsername(user.getUsername()))) throw new CustomException("用户名已存在");
        if (ObjectUtil.isEmpty(user.getSubject())) throw new CustomException("请选择学科");
        if (ObjectUtil.isEmpty(user.getRole())) user.setRole("STUDENT");
        if (ObjectUtil.isEmpty(user.getPassword())) user.setPassword("123456");
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password, String subject, String loginType) {
        User dbUser = userMapper.selectByUsername(username);
        if (ObjectUtil.isNull(dbUser) || !password.equals(dbUser.getPassword())) throw new CustomException("账号或密码错误");
        if (ObjectUtil.isNotEmpty(subject) && !subject.equals(dbUser.getSubject())) throw new CustomException("不可跨学科登录");
        if (ObjectUtil.isNotEmpty(loginType) && !loginType.equals(dbUser.getRole())) throw new CustomException("登录身份与账号角色不匹配");
        return dbUser;
    }

    public void add(User user) { register(user); }
    public void updateById(User user) { userMapper.updateById(user); }
    public void deleteById(Integer id) { userMapper.deleteById(id); }
    public User selectById(Integer id) { return userMapper.selectById(id); }
    public List<User> selectAll(User user) { return userMapper.selectAll(user); }
    public PageInfo<User> selectPage(User user, Integer pageNum, Integer pageSize) { PageHelper.startPage(pageNum, pageSize); return PageInfo.of(userMapper.selectAll(user)); }
}
