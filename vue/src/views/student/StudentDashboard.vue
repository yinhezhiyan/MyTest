<template>
  <div>
    <el-card shadow="never" style="margin-bottom:12px">
      <h2 style="margin:0">今日学习中心</h2>
      <div v-if="overview" style="color:#666;margin-top:8px">累计答题 {{ overview.totalAnswers }}，正确率 {{ overview.accuracy }}%，历史推荐 {{ overview.recommendedCount }}</div>
    </el-card>

    <el-row :gutter="14">
      <el-col :span="12" v-for="item in cards" :key="item.path" style="margin-bottom:14px">
        <el-card shadow="hover" class="entry-card">
          <div class="title">{{ item.title }}</div>
          <div class="desc">{{ item.desc }}</div>
          <el-button type="primary" @click="go(item.path)">进入</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'
const overview = ref(null)
request.get('/api/statistics/learning-overview').then(res => overview.value = res.data)
const cards = [
  { title: '个性拓展', desc: '根据最近答题情况，推荐今日拓展习题', path: '/student/extend' },
  { title: '开始练习', desc: '随机抽取当前学科选择题，答对后自动下一题', path: '/student/practice' },
  { title: '习题推荐', desc: '基于协同过滤+知识图谱的融合推荐题目', path: '/student/recommend' },
  { title: '答题记录', desc: '查看每次作答、选项与对错情况', path: '/student/records' }
]
const go = (path) => router.push(path)
</script>

<style scoped>
.entry-card .title { font-size: 18px; font-weight: 700; margin-bottom: 8px; }
.entry-card .desc { color: #666; margin-bottom: 12px; min-height: 40px; }
</style>
