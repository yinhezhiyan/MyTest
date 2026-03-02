# IntelliJ 运行说明（修复“module not specified / JDK isn't specified”）

如果你看到：
- `module not specified`
- `JDK isn't specified for module 'springboot'`

请按下面顺序：

1. 打开 **Project Structure -> Project**，把 Project SDK 设为 **JDK 21**。
2. 在 Maven 面板重新导入 `springboot/pom.xml`。
3. 直接使用项目自带 Run Config：
   - `SpringbootApplication (IDEA)`（标准 Application 方式，已绑定 `code2026` 模块）
   - `Backend SpringBoot (Maven)`（会在 `springboot` 目录执行 `spring-boot:run`）
   - `Frontend Vite Dev`（会在 `vue` 目录执行 `npm run dev`）

> 这些配置已放在仓库 `.run/` 下，可避免 Application 配置里“module not specified”的问题。
