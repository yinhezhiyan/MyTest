package com.example.service;

import cn.hutool.core.util.ObjectUtil;
import com.example.context.UserContext;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.enums.RoleEnum;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.utils.JwtUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Resource
    private AdminMapper adminMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void add(Admin admin) {
        Account current = UserContext.get();
        requireAdmin(current);
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setSubject(current.getSubject());
        Admin dbAdmin = adminMapper.selectByIdentity(admin.getUsername(), admin.getRole(), admin.getSubject());
        if (ObjectUtil.isNotNull(dbAdmin)) {
            throw new CustomException("该科目下账号已存在");
        }
        if (ObjectUtil.isEmpty(admin.getPassword())) {
            admin.setPassword("admin");
        }
        if (ObjectUtil.isEmpty(admin.getName())) {
            admin.setName(admin.getUsername());
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        adminMapper.insert(admin);
    }

    public void deleteById(Integer id) {
        Account current = UserContext.get();
        requireAdmin(current);
        Admin dbAdmin = adminMapper.selectById(id);
        if (dbAdmin == null || !current.getSubject().equals(dbAdmin.getSubject())) {
            throw new CustomException("无权限操作该学科数据");
        }
        adminMapper.deleteById(id);
    }

    public void updateById(Admin admin) {
        Account current = UserContext.get();
        requireAdmin(current);
        Admin dbAdmin = adminMapper.selectById(admin.getId());
        if (dbAdmin == null || !current.getSubject().equals(dbAdmin.getSubject())) {
            throw new CustomException("无权限操作该学科数据");
        }
        admin.setRole(RoleEnum.ADMIN.name());
        admin.setSubject(current.getSubject());
        if (ObjectUtil.isNotEmpty(admin.getPassword()) && !admin.getPassword().startsWith("$2a$")) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        adminMapper.updateById(admin);
    }

    public Admin selectById(Integer id) {
        Account current = UserContext.get();
        requireLogin(current);
        Admin admin = adminMapper.selectById(id);
        if (admin == null || !current.getSubject().equals(admin.getSubject())) {
            throw new CustomException("无权限查看该学科数据");
        }
        return admin;
    }

    public List<Admin> selectAll(Admin admin) {
        Account current = UserContext.get();
        requireAdmin(current);
        admin.setSubject(current.getSubject());
        admin.setRole(RoleEnum.ADMIN.name());
        return adminMapper.selectAll(admin);
    }

    public PageInfo<Admin> selectPage(Admin admin, Integer pageNum, Integer pageSize) {
        Account current = UserContext.get();
        requireAdmin(current);
        admin.setSubject(current.getSubject());
        admin.setRole(RoleEnum.ADMIN.name());
        PageHelper.startPage(pageNum, pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }

    public Account login(Account account) {
        if (ObjectUtil.hasEmpty(account.getUsername(), account.getPassword(), account.getRole(), account.getSubject())) {
            throw new CustomException("请完整选择身份、科目并输入账号密码");
        }
        Admin dbAdmin = adminMapper.selectByIdentity(account.getUsername(), account.getRole(), account.getSubject());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException("用户不存在或不属于该身份/科目");
        }
        if (!passwordEncoder.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new CustomException("账号或密码错误");
        }
        dbAdmin.setToken(JwtUtils.createToken(dbAdmin));
        dbAdmin.setPassword(null);
        return dbAdmin;
    }

    public void registerStudent(Account account) {
        if (ObjectUtil.hasEmpty(account.getUsername(), account.getPassword(), account.getSubject())) {
            throw new CustomException("请填写完整信息");
        }
        account.setRole(RoleEnum.STUDENT.name());
        Admin dbAdmin = adminMapper.selectByIdentity(account.getUsername(), account.getRole(), account.getSubject());
        if (ObjectUtil.isNotNull(dbAdmin)) {
            throw new CustomException("该科目下账号已存在");
        }
        Admin student = new Admin();
        student.setUsername(account.getUsername());
        student.setName(ObjectUtil.isEmpty(account.getName()) ? account.getUsername() : account.getName());
        student.setPassword(passwordEncoder.encode(account.getPassword()));
        student.setRole(RoleEnum.STUDENT.name());
        student.setSubject(account.getSubject());
        adminMapper.insert(student);
    }

    public void updatePassword(Account account) {
        Account current = UserContext.get();
        requireLogin(current);
        Admin dbAdmin = adminMapper.selectByIdentity(current.getUsername(), current.getRole(), current.getSubject());
        if (ObjectUtil.isNull(dbAdmin)) {
            throw new CustomException("用户不存在");
        }
        if (!passwordEncoder.matches(account.getPassword(), dbAdmin.getPassword())) {
            throw new CustomException("原密码错误");
        }
        dbAdmin.setPassword(passwordEncoder.encode(account.getNewPassword()));
        adminMapper.updateById(dbAdmin);
    }

    private void requireAdmin(Account current) {
        requireLogin(current);
        if (!RoleEnum.ADMIN.name().equals(current.getRole())) {
            throw new CustomException("仅管理员可操作");
        }
    }

    private void requireLogin(Account current) {
        if (current == null) {
            throw new CustomException("401");
        }
    }
}
