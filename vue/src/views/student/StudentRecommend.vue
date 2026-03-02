<template>
  <div class="card">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <h3>习题推荐</h3>
      <el-button type="primary" @click="load">刷新推荐</el-button>
    </div>
    <el-table :data="list" style="margin-top:10px" stripe>
      <el-table-column prop="exercise.title" label="题目" min-width="220" />
      <el-table-column prop="exercise.knowledgePointName" label="知识点" width="140" />
      <el-table-column prop="reason" label="推荐理由" min-width="280" />
      <el-table-column label="操作" width="120">
        <template #default="scope"><el-button @click="goPractice(scope.row.exercise.id)">去练习</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'
const list = ref([])
const load = () => request.get('/api/student/recommendations', { params: { limit: 10 } }).then(res => list.value = res.data || [])
const goPractice = (id) => router.push(`/student/practice?id=${id}`)
load()
</script>
