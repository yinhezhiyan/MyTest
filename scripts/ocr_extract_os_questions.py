#!/usr/bin/env python3
import re
import json
from pathlib import Path
import fitz
import numpy as np
from rapidocr_onnxruntime import RapidOCR

ROOT = Path(__file__).resolve().parents[1]
PDF = ROOT / 'docs/question-pdf/operating-systems.pdf'
OUT_JSON = ROOT / 'data/question-bank/os-ocr.json'
OUT_SQL = ROOT / 'docs/sql/reset-db-and-import-os-ocr.sql'

HEADER_PATTERNS = [r'^微信公众号', r'^\d+\s*王道\s*·\s*OS', r'^\d+$', r'^目录$']
SECTION_PAT = re.compile(r'^(\d+\.\d+)\s*([\u4e00-\u9fa5A-Za-z0-9/（）()\-·\s]+)$')
QSTART_PAT = re.compile(r'^(\d{1,3})[\.．、]\s*(.*)$')
OPT_PAT = re.compile(r'^([ABCD])[\.．]\s*(.*)$')


def normalize(text: str) -> str:
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def should_skip(line: str) -> bool:
    s = line.strip()
    if not s:
        return True
    for p in HEADER_PATTERNS:
        if re.search(p, s):
            return True
    if re.match(r'^答案所在页', s):
        return True
    return False


def slug(ch: str) -> str:
    ch = re.sub(r'[^\w\u4e00-\u9fa5]+', '-', ch.lower()).strip('-')
    return ch or 'chapter'


def split_options(lines):
    stem_lines = []
    opts = {'A': '', 'B': '', 'C': '', 'D': ''}
    cur_opt = None
    for ln in lines:
        m = OPT_PAT.match(ln)
        if m:
            cur_opt = m.group(1)
            opts[cur_opt] = m.group(2).strip()
        else:
            if cur_opt:
                opts[cur_opt] = (opts[cur_opt] + ' ' + ln).strip()
            else:
                stem_lines.append(ln)
    return ' '.join(stem_lines).strip(), opts


def run():
    ocr = RapidOCR()
    doc = fitz.open(PDF)
    questions = []
    current_section = '未分类'

    for page_idx in range(3, min(len(doc), 74)):
        page = doc[page_idx]
        print(f'OCR page {page_idx+1}/{len(doc)}', flush=True)
        pix = page.get_pixmap(dpi=120)
        img = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.height, pix.width, pix.n)
        res, _ = ocr(img)
        if not res:
            continue

        lines = sorted(res, key=lambda x: (sum(p[1] for p in x[0]) / 4.0, sum(p[0] for p in x[0]) / 4.0))
        txts = [normalize(x[1]) for x in lines if x[1] and not should_skip(x[1])]

        current_q = None
        q_lines = []

        def flush_question():
            nonlocal current_q, q_lines
            if current_q is None:
                return
            stem, opts = split_options(q_lines)
            if stem:
                questions.append({
                    'num': current_q,
                    'page': page_idx + 1,
                    'chapter': current_section,
                    'stem': stem,
                    'options': opts,
                })
            current_q, q_lines = None, []

        for ln in txts:
            sec = SECTION_PAT.match(ln)
            if sec and ('章' not in ln):
                current_section = sec.group(2).strip() or current_section
                continue

            qm = QSTART_PAT.match(ln)
            if qm:
                flush_question()
                current_q = int(qm.group(1))
                remain = qm.group(2).strip()
                if remain:
                    q_lines.append(remain)
                continue

            if current_q is not None:
                q_lines.append(ln)

        flush_question()

    uniq = []
    seen = set()
    for q in questions:
        k = (q['page'], q['num'], q['stem'])
        if k in seen:
            continue
        seen.add(k)
        uniq.append(q)

    out = []
    for i, q in enumerate(uniq, 1):
        out.append({
            'id': f'os-ocr-{i:04d}',
            'subject': 'OS',
            'chapter': q['chapter'],
            'chapterSlug': slug(q['chapter']),
            'stem': q['stem'],
            'options': q['options'],
            'answer': '',
            'analysis': '',
            'difficulty': 2,
            'knowledge_points': [q['chapter'], 'OCR导入'],
            'source_page': q['page'],
            'source_num': q['num']
        })

    OUT_JSON.write_text(json.dumps(out, ensure_ascii=False, indent=2))

    def esc(s: str) -> str:
        return s.replace('\\', '\\\\').replace("'", "\\'")

    sql = []
    sql.append('-- 先确认你在正确数据库执行')
    sql.append('USE `system`;')
    sql.append('SET NAMES utf8mb4;')
    sql.append('SET FOREIGN_KEY_CHECKS = 0;')
    sql.append('')
    sql.append('-- 清理旧结构（你截图中的旧表）')
    sql.append('DROP TABLE IF EXISTS `recommendation_log`;')
    sql.append('DROP TABLE IF EXISTS `user_behavior`;')
    sql.append('DROP TABLE IF EXISTS `knowledge_point`;')
    sql.append('DROP TABLE IF EXISTS `admin`;')
    sql.append('DROP TABLE IF EXISTS `exercise`;')
    sql.append('DROP TABLE IF EXISTS `user_answer`;')
    sql.append('DROP TABLE IF EXISTS `sys_user`;')
    sql.append('')
    sql.append('-- 重建当前项目所需核心表')
    sql.append("""CREATE TABLE `sys_user` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `name` varchar(100),
  `avatar` varchar(255),
  `role` varchar(20) NOT NULL,
  `subject` varchar(20) NOT NULL,
  `status` tinyint DEFAULT 1,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_identity` (`username`,`role`,`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""")
    sql.append("""CREATE TABLE `exercise` (
  `id` varchar(64) PRIMARY KEY,
  `subject` varchar(20) NOT NULL,
  `chapter` varchar(100) NOT NULL,
  `chapter_slug` varchar(100),
  `stem` text NOT NULL,
  `option_a` varchar(255),
  `option_b` varchar(255),
  `option_c` varchar(255),
  `option_d` varchar(255),
  `answer` varchar(4) NOT NULL,
  `analysis` text,
  `difficulty` int DEFAULT 2,
  `knowledge_points` text,
  `attachment_url` varchar(255),
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_exercise_subject` (`subject`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""")
    sql.append("""CREATE TABLE `user_answer` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `subject` varchar(20) NOT NULL,
  `exercise_id` varchar(64) NOT NULL,
  `is_correct` tinyint NOT NULL,
  `chosen_option` varchar(4),
  `correct_answer` varchar(4),
  `answered_at` datetime,
  KEY `idx_answer_user_subject` (`user_id`,`subject`),
  KEY `idx_answer_exercise` (`exercise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""")
    sql.append('')
    sql.append(f'-- OCR导入操作系统题目，共 {len(out)} 题')
    sql.append('INSERT INTO `exercise` (`id`,`subject`,`chapter`,`chapter_slug`,`stem`,`option_a`,`option_b`,`option_c`,`option_d`,`answer`,`analysis`,`difficulty`,`knowledge_points`,`attachment_url`) VALUES')
    rows = []
    for q in out:
        kp = json.dumps(q['knowledge_points'], ensure_ascii=False)
        o = q['options']
        rows.append(
            "('{}','OS','{}','{}','{}','{}','{}','{}','{}','','',2,'{}','')".format(
                esc(q['id']), esc(q['chapter']), esc(q['chapterSlug']), esc(q['stem']),
                esc(o.get('A', '')), esc(o.get('B', '')), esc(o.get('C', '')), esc(o.get('D', '')),
                esc(kp)
            )
        )
    sql.append(',\n'.join(rows) + ';')
    sql.append('')
    sql.append('SET FOREIGN_KEY_CHECKS = 1;')
    sql.append('SELECT DATABASE() AS current_db;')
    sql.append('SELECT COUNT(*) AS total FROM exercise WHERE subject=\'OS\';')
    OUT_SQL.write_text('\n'.join(sql))

    print(f'Generated {len(out)} OCR questions')


if __name__ == '__main__':
    run()
