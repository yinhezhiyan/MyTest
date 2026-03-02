# 项目巡检报告

## 结论
- 前端可构建通过（`npm run build` 成功）。
- 后端当前环境无法完成 Maven 构建/测试（拉取 Spring Boot 父 POM 被 403 拒绝）。
- 登录逻辑从代码看是正常的：用户名+密码匹配，且会校验学科与登录角色。

## 登录账号密码（来自初始化脚本）
- 管理员：
  - `admin_math / admin123`（数学）
  - `admin_english / admin123`（英语）
  - `admin_c / admin123`（C语言程序设计）
- 学生：
  - `stu_math_1 / 123456`、`stu_math_2 / 123456`
  - `stu_eng_1 / 123456`、`stu_eng_2 / 123456`
  - `stu_c_1 / 123456`、`stu_c_2 / 123456`

## 关键说明
- 后端连接 MySQL，默认连接：`localhost:3306/system`，默认账号：`root/123456`。
- 启动时会执行 `classpath:sql/demo_seed.sql` 初始化数据。
- 前端登录页也给出了演示账号：`admin_math/admin123(数学)`。
