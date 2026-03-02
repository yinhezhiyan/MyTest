<template>
  <div class="card">
    <el-table :data="tableData" stripe>
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="exerciseId" label="题目ID" width="100" />
      <el-table-column prop="cfScore" label="CF" width="100" />
      <el-table-column prop="kgScore" label="KG" width="100" />
      <el-table-column prop="hybridScore" label="融合" width="100" />
      <el-table-column prop="reason" label="原因" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'

const tableData = ref([])
request.get('/api/admin/recommendation-logs').then(res => { tableData.value = res.data || [] })
</script>
