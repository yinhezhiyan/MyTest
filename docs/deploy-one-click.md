# 一键启动（本地可点击链接）

> 目标：把前后端 + MySQL 打包成一个可直接访问的链接。

## 方式
使用 `docker compose` 一次性启动三项服务：
- MySQL
- Spring Boot 后端
- Nginx 托管的 Vue 前端（并反向代理后端 API）

## 步骤
1. 安装 Docker / Docker Compose。
2. 在仓库根目录执行：

```bash
docker compose up -d --build
```

3. 启动完成后，直接打开：

- http://localhost:8088

这就是“点一下就能用”的入口链接。

## 停止服务
```bash
docker compose down
```

## 默认说明
- 数据库：`code2026`
- MySQL root 密码：`123456`
- 后端容器会自动初始化表结构并从 `data/question-bank` 导入题库。

> 如果你需要**公网链接**（别人也能访问），还需要把这套服务部署到云服务器/平台，并绑定公网域名。
