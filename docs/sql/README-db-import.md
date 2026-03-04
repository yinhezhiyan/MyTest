# 题库导入脚本怎么选

你当前项目里有两个常见数据库名：

- `code2026`（`application.yml` 默认连接）
- `system`（你在 Navicat 截图里查看的库）

如果你在 **system 库**里看数据，请执行：

```sql
source docs/sql/reset-system-db-and-import-current-question-bank.sql;
```

如果你在 **code2026 库**里跑项目，请执行：

```sql
source docs/sql/reset-db-and-import-current-question-bank.sql;
```

执行完成后建议核对：

```sql
SELECT subject, COUNT(*) AS total FROM exercise GROUP BY subject ORDER BY subject;
```
