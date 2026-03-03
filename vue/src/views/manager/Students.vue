<template>
  <div>
    <div class="card" style="margin-bottom:8px">
      <el-input v-model="name" style="width:260px;margin-right:8px" placeholder="按昵称搜索"/>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <div class="card">
      <el-table :data="tableData" stripe>
        <el-table-column prop="avatar" label="头像" width="90">
          <template #default="scope"><el-avatar :src="scope.row.avatar">{{(scope.row.name || scope.row.username || 'U').slice(0,1)}}</el-avatar></template>
        </el-table-column>
        <el-table-column prop="username" label="账号"/>
        <el-table-column prop="name" label="昵称"/>
        <el-table-column label="操作" width="140">
          <template #default="scope"><el-button link type="primary" @click="openRecords(scope.row)">答题记录</el-button></template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" @current-change="load"/>
    </div>

    <el-dialog v-model="recordVisible" width="70%" title="学生答题记录">
      <div style="margin-bottom:10px">
        <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable @change="loadRecords"/>
      </div>
      <el-table :data="records">
        <el-table-column prop="chapter" label="章节" width="120"/>
        <el-table-column prop="stem" label="题目"/>
        <el-table-column prop="answered_at" label="时间" width="180"/>
        <el-table-column prop="is_correct" label="是否正确" width="100"><template #default="s">{{s.row.is_correct ? '是' : '否'}}</template></el-table-column>
        <el-table-column prop="chosen_option" label="选择" width="90"/>
        <el-table-column prop="correct_answer" label="正确" width="90"/>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import request from '@/utils/request'

const pageNum = ref(1), pageSize = ref(10), total = ref(0), name = ref('')
const tableData = ref([])
const recordVisible = ref(false)
const currentStudentId = ref(null)
const date = ref('')
const records = ref([])

const load = ()=> {
  request.get('/admin/student/selectPage', { params: { pageNum: pageNum.value, pageSize: pageSize.value, name: name.value || undefined } }).then(res => {
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  })
}

const openRecords = (row) => {
  currentStudentId.value = row.id
  date.value = ''
  recordVisible.value = true
  loadRecords()
}

const loadRecords = () => {
  if (!currentStudentId.value) return
  request.get(`/admin/student/records/${currentStudentId.value}`, { params: { date: date.value || undefined } }).then(res => records.value = res.data || [])
}

load()
</script>
