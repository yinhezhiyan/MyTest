# 操作系统题库（OCR版）重置与导入说明

你说得对：你截图里 `system.exercise` 还是旧结构（`title/content/knowledge_point_id`），说明之前脚本没有真正落到当前库结构。

最常见原因：
- 没在 `system` 库执行；
- 没整段执行到 `DROP TABLE`；
- 执行中断后只跑了部分语句。

这次我给你的是 **OCR 全量生成版**，来源是项目内 `docs/question-pdf/operating-systems.pdf`，通过 OCR 扫描题页生成。

---

## 你应该粘贴哪段代码、粘贴到哪里

### 直接执行（推荐）
1. 打开 Navicat，连接 `localhost_3306`。
2. 选中数据库 `system`。
3. 新建查询。
4. 打开文件：`docs/sql/reset-db-and-import-os-ocr.sql`。
5. 复制**全部内容**粘贴执行。

该 SQL 会：
- 强制 `USE system`；
- 删除旧表（包括旧 `exercise`）；
- 按当前项目结构重建 `sys_user / exercise / user_answer`；
- 导入 OCR 识别出的 OS 题目（当前 620 题）。

### 执行后核验
在 Navicat 执行：
```sql
SELECT DATABASE() AS current_db;
SHOW COLUMNS FROM exercise;
SELECT COUNT(*) AS total FROM exercise WHERE subject = 'OS';
```

如果列名是 `id/subject/chapter/stem/option_a...`，且题量是 620，就说明已成功替换掉旧表。

---

## OCR 数据文件（可选）
- OCR 结果 JSON：`data/question-bank/os-ocr.json`
- 生成脚本：`scripts/ocr_extract_os_questions.py`

如果你后续再换 PDF，只要重新跑：
```bash
python scripts/ocr_extract_os_questions.py
```
就会重新生成 SQL。
