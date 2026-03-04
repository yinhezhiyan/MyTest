# 操作系统题库重置与导入说明

你现在可以直接用 `docs/sql/reset-db-and-import-os.sql` 完成“删旧库 + 按当前项目结构重建 + 仅导入操作系统题目”。

## 你要粘贴到哪里？
在 Navicat 中按以下步骤：

1. 连接到 `localhost_3306`。
2. 选择数据库 `system`。
3. 点击 **新建查询**。
4. 打开并复制 `docs/sql/reset-db-and-import-os.sql` 全部内容。
5. 粘贴到查询窗口，点击 **运行**。

## 执行后效果
- 会删除截图中旧结构相关表（如 `knowledge_point`、旧 `exercise` 等）。
- 会按当前 SpringBoot 项目需要重建：
  - `sys_user`
  - `exercise`
  - `user_answer`
- 只导入 `OS` 学科题目（120题）。

## 说明
- 本脚本中的 OS 题目数据来源于项目内 `data/question-bank/os.json` 的全量题目，已转成可直接执行的 SQL `INSERT`。
- 执行末尾有校验 SQL：
  - `SELECT subject, COUNT(*) AS total FROM exercise GROUP BY subject;`
  - 预期只看到 `OS | 120`。
