<template>
  <div>
    <div class="card" style="line-height: 30px; margin-bottom: 10px">
      <div style="margin-bottom: 12px; font-size: 22px">基于协同过滤与知识图谱的个性化习题推荐系统（演示版）</div>
      <div>当前登录角色：<b>{{ user.role }}</b>，欢迎 {{ user.name || user.username }}。</div>
      <div v-if="overview">学习进度：已答题 {{ overview.totalAnswers }} 题，正确 {{ overview.correctAnswers }} 题，正确率 {{ overview.accuracy }}%。</div>
      <div v-if="overview">历史推荐条数：{{ overview.recommendedCount }}。</div>
    </div>

    <div class="card">
      <div style="font-weight: 600; margin-bottom: 6px">演示建议</div>
      <div>1）先使用学生账号登录，点击“我的推荐”展示推荐结果与推荐理由。</div>
      <div>2）再用管理员账号登录，查看用户、题库、知识点与推荐日志管理能力。</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const overview = ref(null)

request.get('/api/statistics/learning-overview').then(res => {
  if (res.code === '200') {
    overview.value = res.data
  }
})
</script>
