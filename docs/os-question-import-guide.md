# 操作系统题库（按你提供图片识别）导入说明

这次不再使用 `docs/question-pdf/operating-systems.pdf` 的 OCR 脚本，改为基于你在对话里提供的十几张截图整理题库。

## 已做的调整
- 已删除旧的 OCR 脚本：`scripts/ocr_extract_os_questions.py`。
- 已移除此前生成的 PDF-OCR 产物：
  - `data/question-bank/os-ocr.json`
  - `docs/sql/reset-db-and-import-os-ocr.sql`
- 新增图片识别后的题库：`data/question-bank/os-image-ocr.json`。
- 新增可直接执行的重置+导入 SQL：`docs/sql/reset-db-and-import-os-images.sql`。

## 你要粘贴到哪里？
1. Navicat 连接 `localhost_3306`
2. 选择库 `system`
3. 新建查询
4. 打开 `docs/sql/reset-db-and-import-os-images.sql`
5. 全部复制粘贴并执行

## 执行后检查
```sql
SELECT DATABASE() AS current_db;
SHOW COLUMNS FROM exercise;
SELECT COUNT(*) AS total FROM exercise WHERE subject='OS';
```

## 说明
- 脚本会先删旧表（包含旧结构 `exercise(title/content/knowledge_point_id...)`），再按当前项目结构重建。
- 当前图片识别并整理入库题目数：73 题（章节：进程与线程）。
- 题目答案字段留空，后续你如果要我继续把答案也补齐，我可以继续按你给图批量补。
