<template>
  <div class="card">
    <h3>我的做题记录</h3>
    <el-table :data="list" stripe>
      <el-table-column prop="exerciseId" label="习题ID" />
      <el-table-column prop="actionType" label="行为" />
      <el-table-column prop="isCorrect" label="是否正确" />
      <el-table-column prop="score" label="得分" />
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
const list = ref([])
request.get('/api/student/behaviors').then(res => list.value = res.data?.list || [])
</script>
