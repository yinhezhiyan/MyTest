# 项目巡检报告

## 结论
- 前端可正常构建（`npm run build` 成功）。
- 后端在当前环境无法完成 Maven 测试/构建：拉取 Spring Boot 父 POM 时被 Maven Central 返回 `403 Forbidden`。
- 因后端依赖解析失败，当前环境下无法直接验证后端服务完整启动。

## 我实际执行的检查

### 1) 前端构建检查
在 `vue/` 目录执行：
- `npm run build`

结果：
- 构建成功，Vite 完成打包并产出 `dist/`。
- 有 Sass legacy-js-api deprecation 提示，以及 chunk size 提示（告警，不影响本次构建通过）。

### 2) 后端测试/构建检查
在 `springboot/` 目录执行：
- `mvn -q test`

结果：
- 失败，核心错误为：
  - `Non-resolvable parent POM`
  - 无法从 `https://repo.maven.apache.org/maven2` 拉取 `org.springframework.boot:spring-boot-starter-parent:3.3.1`
  - HTTP 状态：`403 Forbidden`

## 结论解读（能否正常运行）
- **前端：可以正常构建，运行前端本身没问题。**
- **后端：在当前环境下不能正常构建，因此不能判定可正常启动。**
- 整体项目要“完整正常运行”（前后端联调）目前被后端依赖拉取问题阻塞。

## 建议你本地优先排查
1. 检查 Maven 网络与镜像配置（`settings.xml`）是否被公司代理/仓库策略拦截了 Maven Central。  
2. 若在内网，改用可访问的私有镜像仓库（如 Nexus/阿里云镜像）并确保 Spring Boot 3.3.1 可下载。  
3. 修复后重试：
   - `cd springboot && mvn clean test`
   - `cd springboot && mvn spring-boot:run`
