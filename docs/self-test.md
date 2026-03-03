# 自测路径

1. 登录页选择科目：数据结构 / 操作系统 / 计网 / 计组。
2. 管理员登录后调用导入接口：
```http
POST /admin/question-bank/import
Authorization: Bearer <admin-token>
Content-Type: application/json

{"subject":"DS","filePath":"data/question-bank/ds.json"}
```
3. 学生登录后访问：
- `/manager/{subject}/daily`
- `/manager/{subject}/practice`
- `/manager/{subject}/recommend`
- `/manager/{subject}/records`

## 推荐接口
```http
GET /api/recommendations?topN=5
Authorization: Bearer <student-token>
```
