<template>
  <div class="card">
    <div style="margin-bottom: 8px">
      <el-button type="primary" @click="load">生成个性化推荐</el-button>
    </div>
    <el-table :data="tableData" stripe>
      <el-table-column prop="exercise.title" label="题目" min-width="240" />
      <el-table-column prop="cfScore" label="CF评分" width="100" />
      <el-table-column prop="kgScore" label="KG评分" width="100" />
      <el-table-column prop="hybridScore" label="融合评分" width="100" />
      <el-table-column prop="reason" label="推荐解释" min-width="220" />
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
</script>
