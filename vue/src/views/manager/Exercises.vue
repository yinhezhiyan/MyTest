<template>
  <div>
    <div class="card" style="margin-bottom: 8px">
      <el-input v-model="query.title" placeholder="题目关键字" style="width: 260px; margin-right: 8px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="addSample">新增示例题</el-button>
    </div>
    <div class="card">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题目" min-width="220" />
        <el-table-column prop="difficulty" label="难度" width="120" />
        <el-table-column prop="knowledgePointName" label="知识点" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const query = reactive({ title: '' })
const tableData = ref([])

const load = () => {
  request.get('/api/exercises/page', { params: { ...query, pageNum: 1, pageSize: 50 } }).then(res => {
    tableData.value = res.data?.list || []
  })
}

const addSample = () => {
  request.post('/api/exercises', {
    title: '示例题-' + Date.now(),
    content: '请简述协同过滤的核心思想',
    difficulty: '中等',
    knowledgePointId: 1
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('新增成功')
      load()
    }
  })
}

load()
</script>
