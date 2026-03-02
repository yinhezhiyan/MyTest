<template>
  <div>
    <div class="card" style="margin-bottom: 8px">
      <el-input v-model="name" placeholder="知识点名称" style="width: 240px; margin-right: 8px" />
      <el-button type="primary" @click="add">新增知识点</el-button>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="card">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="说明" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const name = ref('')
const tableData = ref([])

const load = () => {
  request.get('/api/knowledge-points').then(res => { tableData.value = res.data || [] })
}

const add = () => {
  if (!name.value) return
  request.post('/api/knowledge-points', { name: name.value, description: '自动创建' }).then(res => {
    if (res.code === '200') {
      ElMessage.success('新增成功')
      name.value = ''
      load()
    }
  })
}

load()
</script>
