# 部署指南（最小改动可上线）

本项目已适配：
- 前端（Vue + Vite）部署到 **Vercel**
- 后端（Spring Boot）部署到 **Render**
- 数据库使用 **Aiven MySQL**

---

## 0. 代码已完成的改造（你无需再改代码）

- 后端数据库连接支持环境变量：`DB_URL` / `DB_USERNAME` / `DB_PASSWORD`。
- 后端端口支持 Render 的 `PORT`（`server.port=${PORT:9090}`）。
- 新增健康检查：`GET /health` 返回 `{ "status": "ok" }`。
- CORS 改为可配置，仅建议放开：本地前端 + Vercel 域名。
- JWT 密钥改为环境变量 `JWT_SECRET`（不再硬编码在仓库中）。
- 前端 API 地址统一为：`VITE_API_BASE_URL`（已移除代码中的硬编码 localhost）。
- 新增 `vercel.json`，用于 SPA 路由刷新不 404。
- 新增 `.env.example` 作为环境变量模板。
- 后端新增 Maven Wrapper，可在 Render 用 `./mvnw` 构建。

---

## 1. Aiven MySQL：创建数据库并导入数据

> 推荐先把本地可用数据导入 Aiven，再部署后端。

### 1.1 从本地 MySQL 导出（结构+数据）

#### 方式 A：命令行（推荐）
```bash
mysqldump -h 127.0.0.1 -P 3306 -u root -p --databases code2026 --routines --triggers --events > code2026_full.sql
```

#### 方式 B：Navicat
- 右键本地数据库 `code2026`
- 选择「转储 SQL 文件」
- 勾选「结构 + 数据」导出

### 1.2 导入到 Aiven MySQL

在 Aiven 控制台创建 MySQL 服务后，拿到：
- Host
- Port
- Database（一般是 `defaultdb`，也可自建）
- Username（常见 `avnadmin`）
- Password
- SSL 要求（Aiven 通常要求 SSL）

#### 方式 A：命令行导入
```bash
mysql -h <AIVEN_HOST> -P <AIVEN_PORT> -u <AIVEN_USER> -p --ssl-mode=REQUIRED < code2026_full.sql
```

#### 方式 B：Navicat 导入
- 新建连接（填 Aiven 主机、端口、用户名、密码）
- SSL 选项按 Aiven 控制台说明开启
- 执行导出的 SQL 文件

### 1.3 JDBC 连接串建议

Render 环境变量 `DB_URL` 建议格式：
```text
jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/<DB_NAME>?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

> 如果你明确使用中国时区，也可改 `serverTimezone=GMT%2B8`。

---

## 2. 后端部署到 Render（Spring Boot）

在 Render 新建 **Web Service**（连接你的 GitHub 仓库）：

- **Root Directory**: `springboot`
- **Environment**: `Java`
- **Build Command**:
  ```bash
  ./mvnw clean package -DskipTests
  ```
- **Start Command**:
  ```bash
  java -jar target/springboot-0.0.1-SNAPSHOT.jar
  ```

> 若后续版本号变化，请把 jar 名字改成实际文件名。

### 2.1 Render 环境变量（必须）

把以下变量填到 Render：

- `PORT` = `8080`
- `DB_URL` = `jdbc:mysql://...`（Aiven 连接串）
- `DB_USERNAME` = `...`
- `DB_PASSWORD` = `...`
- `JWT_SECRET` = `至少 32 位随机字符串`
- `JWT_EXPIRE_MILLIS` = `86400000`
- `FILE_BASE_URL` = `https://<你的-render-域名>`
- `CORS_ALLOWED_ORIGIN_PATTERNS` = `http://localhost:5173,http://127.0.0.1:5173,https://<你的-vercel-域名>`

### 2.2 后端上线验证

部署完成后访问：
- `https://<你的-render-域名>/health`

期望返回：
```json
{"status":"ok"}
```

---

## 3. 前端部署到 Vercel（Vue + Vite）

在 Vercel 导入同一个 GitHub 仓库：

- **Framework Preset**: `Vite`
- **Root Directory**: `vue`
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install`（默认即可）

### 3.1 Vercel 环境变量

- `VITE_API_BASE_URL` = `https://<你的-render-域名>`

> 已配置 `vercel.json`，支持 SPA 刷新路由不 404。

---

## 4. 本地开发保持不变

- 前端本地：`vue/.env.development` 已默认 `VITE_API_BASE_URL=http://localhost:9090`
- 后端本地：`application.yml` 默认使用 localhost MySQL（`root/123456` + `code2026`）
- 本地仍可用 `localhost` 前后端联调

---

## 5. 上线验证 Checklist（给老师演示前逐项勾选）

1. [ ] 后端健康检查可用：`GET /health` 返回 `status=ok`
2. [ ] 前端 Vercel 链接可打开（登录页正常）
3. [ ] 前端登录成功（接口可达）
4. [ ] 题库列表/练习/推荐等核心接口能返回数据
5. [ ] 浏览器控制台无 CORS 报错
6. [ ] Render 日志无数据库连接错误
7. [ ] Aiven 中可查到核心业务表和数据（如账号、题库相关表）

---

## 6. 你在三个平台需要“复制粘贴”的关键内容

### Render
- Root Directory: `springboot`
- Build Command: `./mvnw clean package -DskipTests`
- Start Command: `java -jar target/springboot-0.0.1-SNAPSHOT.jar`

### Vercel
- Root Directory: `vue`
- Build Command: `npm run build`
- Output Directory: `dist`
- Env: `VITE_API_BASE_URL=https://<render-domain>`

### Aiven
- 从控制台复制 Host/Port/User/Password/DB
- 组装 `DB_URL`：
  `jdbc:mysql://<host>:<port>/<db>?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true`

