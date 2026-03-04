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
COMPOSE_PROJECT_NAME=mytest docker compose down
```

> 请勿使用 `docker compose down -v`，否则会删除数据库卷并清空学生注册信息、答题记录。

## 默认说明
- 数据库：`code2026`
- MySQL root 密码：`123456`
- 后端容器会自动初始化表结构并从 `data/question-bank` 导入题库。

> 如果你需要**公网链接**（别人也能访问），还需要把这套服务部署到云服务器/平台，并绑定公网域名。


## 一键脚本（推荐）
在仓库根目录执行：

- Linux / macOS（或已安装 Git Bash/WSL 的环境）：

```bash
bash scripts/package-and-link.sh
```

- Windows PowerShell（推荐，不依赖 WSL）：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-and-link.ps1
```

脚本会自动构建并启动服务，然后直接打印可访问链接（localhost + 局域网IP）。脚本固定使用 `mytest` 项目名并复用数据库卷，保证学生注册信息、答题记录长期保留。

如果你要一个可分享的公网临时链接（例如发给别人直接点开）：

```bash
# Linux / macOS
bash scripts/package-and-link.sh --public
```

```powershell
# Windows PowerShell
powershell -ExecutionPolicy Bypass -File .\scripts\package-and-link.ps1 -Public
```

该命令会调用 `localtunnel` 生成公网临时 URL（终端关闭后链接失效）。


## 常见问题
- 如果你在 Windows 里执行 `bash scripts/package-and-link.sh`，但系统提示“WSL 没有已安装的发行版”，说明脚本并未真正执行成功，自然不会打印访问链接。
- 这种情况下请改用 `package-and-link.ps1`。
