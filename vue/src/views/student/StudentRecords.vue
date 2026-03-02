<template>
  <div class="card">
    <h3>答题记录</h3>
    <el-table :data="list" stripe style="margin-top:10px">
      <el-table-column prop="attemptTime" label="时间" width="180" />
      <el-table-column prop="exerciseTitle" label="题目" min-width="220" />
      <el-table-column prop="chosen" label="选择" width="80" />
      <el-table-column label="结果" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.isCorrect === 1 ? 'success' : 'danger'">{{ scope.row.isCorrect === 1 ? '正确' : '错误' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="得分" width="100" />
    </el-table>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
const list = ref([])
request.get('/api/student/behaviors').then(res => list.value = res.data?.list || [])
</script>
