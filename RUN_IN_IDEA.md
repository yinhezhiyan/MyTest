# IntelliJ 运行说明（修复“module not specified / JDK isn't specified / ClassNotFoundException”）

如果你看到：
- `module not specified`
- `JDK isn't specified for module 'springboot'`
- `错误: 找不到或无法加载主类 com.example.SpringbootApplication`

请按下面顺序：

1. 打开 **Project Structure -> Project**，把 Project SDK 设为 **JDK 21**。
2. 在 Maven 面板重新导入 `springboot/pom.xml`。
   - 看到模块列表里有 `springboot` 再继续（没有就点 Maven 面板的 Reimport）。
3. 直接使用项目自带 Run Config（推荐 Maven 方式，避免 classpath/module 问题）：
   - `SpringbootApplication (springboot module)`（Spring Boot 配置）
   - `Backend SpringBoot (Maven)`（会在 `springboot` 目录执行 `spring-boot:run`）
   - `Frontend Vite Dev`（会在 `vue` 目录执行 `npm run dev`）
4. **不要再用旧的 Java Application 临时配置**（它会走 `com.example.SpringbootApplication` 直启，若 classpath 未正确构建就会报 `ClassNotFoundException`）。

## 重点：你这类报错通常是“启动方式和构建产物不一致”

你贴出的命令里 classpath 是：
- `...\demo\build\classes\java\main`

这是 **Gradle 输出目录**；而你当前后端模块同时提供了 **Maven 运行方式**（`springboot/pom.xml`），常见情况是：
- 你用 Java Application 直启（走 Gradle classpath）
- 但实际并没有先产出 `build/classes/...` 的 `com/example/SpringbootApplication.class`

就会出现 `ClassNotFoundException`。

## 两种稳定运行方式（二选一）

### 方案 A（推荐）：Maven 方式启动
在 IntelliJ 里直接运行：
- `Backend SpringBoot (Maven)`

或者命令行：
```bash
cd springboot
mvn spring-boot:run
```

### 方案 B：保留 Java Application 方式
如果你一定要用 Java Application 直启，请先在项目根目录执行：
```bash
./gradlew clean classes
```
然后再运行 `com.example.SpringbootApplication`（classpath 走 `build/classes/java/main`）。

## 自检清单

1. Run Configuration 的 `Use classpath of module` 是否为 `springboot`。
2. `com/example/SpringbootApplication.class` 是否存在于你当前启动方式对应的输出目录：
   - Maven：`springboot/target/classes`
   - Gradle：`build/classes/java/main`
3. 有多个同名 `SpringbootApplication` 时，删除旧配置，只保留 `.run/` 下的共享配置。

> 这些配置已放在仓库 `.run/` 下，可避免 Application 配置里“module not specified”和 classpath 混乱的问题。
