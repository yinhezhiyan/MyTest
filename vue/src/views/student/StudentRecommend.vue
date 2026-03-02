<template>
  <div class="card">
    <el-button type="primary" @click="load">刷新推荐</el-button>
    <el-table :data="list" style="margin-top:10px" stripe>
      <el-table-column prop="exercise.title" label="题目" />
      <el-table-column prop="exercise.knowledgePointName" label="知识点" />
      <el-table-column prop="reason" label="推荐解释" min-width="300" />
      <el-table-column><template #default="scope"><el-button @click="goPractice(scope.row.exercise.id)">去练习</el-button></template></el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'
const list = ref([])
const load = () => request.get('/api/student/recommendations').then(res => list.value = res.data || [])
const goPractice = (id) => router.push(`/student/practice?id=${id}`)
load()
</script>
