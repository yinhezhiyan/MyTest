# IntelliJ 运行说明（修复“module not specified / JDK isn't specified”）

如果你看到：
- `module not specified`
- `JDK isn't specified for module 'springboot'`

请按下面顺序：

1. 打开 **Project Structure -> Project**，把 Project SDK 设为 **JDK 21**。
2. 在 Maven 面板重新导入 `springboot/pom.xml`。
   - 看到模块列表里有 `springboot` 再继续（没有就点 Maven 面板的 Reimport）。
3. 直接使用项目自带 Run Config（推荐 Maven 方式，避免 classpath/module 问题）：
   - `SpringbootApplication`（Maven 方式，名称与原先一致，直接运行）
   - `Backend SpringBoot (Maven)`（同上，会在 `springboot` 目录执行 `spring-boot:run`）
   - `Frontend Vite Dev`（会在 `vue` 目录执行 `npm run dev`）

4. **不要再用旧的 Java Application 临时配置**（它会走 `com.example.SpringbootApplication` 直启，若 classpath 未正确构建就会报 `ClassNotFoundException`）。

5. 如果你下拉框里有多个同名 `SpringbootApplication`，请删除旧的（Java 图标那个），只保留 Maven 图标配置。

> 这些配置已放在仓库 `.run/` 下，可避免 Application 配置里“module not specified”的问题。
