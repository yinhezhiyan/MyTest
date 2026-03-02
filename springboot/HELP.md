# SpringBoot 运行指引（含 ClassNotFoundException 排查）

> 适用于报错：`Could not find or load main class com.example.SpringbootApplication`

## 0) 你这个报错最常见的直接原因
从你截图看，当前工作区有多个目录（如 `demo`、`springboot`）。这类场景下，IDEA 很容易把运行配置绑定到**错误模块**，导致 classpath 里没有 `springboot` 的编译产物，最终出现 `ClassNotFoundException`。

本仓库已新增共享运行配置：
- `.run/SpringbootApplication.run.xml`

它固定使用：
- module = `springboot`
- main class = `com.example.SpringbootApplication`

## 一、先用 Maven 验证能否编译
在 `springboot/` 目录执行：

```bash
mvn clean compile
```

如果这一步失败（例如依赖下载失败），IDEA 运行时就会出现主类找不到，因为 `target/classes` 不会产出完整 class 文件。

## 二、IDEA 正确启动方式（重点）
1. 右侧 Maven 面板执行一次：`Reload All Maven Projects`。
2. 确认 Project SDK 使用 **JDK 21**（与 `pom.xml` 一致）。
3. 在 `Project Structure -> Modules` 中确认 `src/main/java` 被标记为 **Sources**。
4. 打开 Run/Debug Configurations：
   - 选择 `SpringbootApplication (springboot module)`（仓库共享的 `.run` 配置）。
   - 或手工新建时，务必把 `Use classpath of module` 指向 `springboot`（不是 `demo` 或其他模块）。
5. 执行 `Build -> Rebuild Project` 后再启动。

## 三、仍报 ClassNotFound 时快速自检
在 `springboot/` 目录执行：

```bash
# 1) 检查主类源码是否存在
ls src/main/java/com/example/SpringbootApplication.java

# 2) 编译后检查主类 class 是否生成
ls target/classes/com/example/SpringbootApplication.class
```

- 第 1 条存在、第 2 条不存在：说明是**编译没有通过**，先解决 Maven 依赖/编译错误。
- 两条都存在但 IDE 仍报错：通常是**运行配置 classpath 绑错模块**，改成 `springboot` 即可。

## 四、数据库相关（启动后常见）
应用默认连接：

- Host: `localhost`
- Port: `3306`
- DB: `system`
- User: `root`
- Password: `123456`

可通过环境变量覆盖：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`、`DB_PASSWORD`。
