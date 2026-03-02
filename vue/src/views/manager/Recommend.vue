<template>
  <div class="card">
    <div style="margin-bottom: 8px">
      <el-button type="primary" @click="load">生成个性化推荐</el-button>
    </div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="exercise.title" label="题目" min-width="220" />
      <el-table-column prop="exercise.knowledgePointName" label="知识点" min-width="120" />
      <el-table-column prop="exercise.difficulty" label="难度" width="90" />
      <el-table-column prop="cfScore" label="CF评分" width="95" />
      <el-table-column prop="kgScore" label="KG评分" width="95" />
      <el-table-column prop="hybridScore" label="融合评分" width="95" />
      <el-table-column prop="reason" label="推荐理由" min-width="200" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const tableData = ref([])
const load = () => {
  request.get('/api/recommendations').then(res => { tableData.value = res.data || [] })
}

load()
</script>
