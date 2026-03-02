<template>
  <div>
    <div class="card" style="margin-bottom: 8px">
      <el-input v-model="query.username" placeholder="用户名" style="width: 240px; margin-right: 8px" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="card">
      <el-table :data="tableData" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="grade" label="年级" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'

const query = reactive({ username: '', role: 'STUDENT' })
const tableData = ref([])

const load = () => {
  request.get('/api/admin/users', { params: { ...query, pageNum: 1, pageSize: 50 } }).then(res => {
    tableData.value = res.data?.list || []
  })
}

load()
</script>
