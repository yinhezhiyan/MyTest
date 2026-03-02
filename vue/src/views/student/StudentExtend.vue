<template>
  <div class="card">
    <h3>今日个性拓展</h3>
    <el-row :gutter="12" style="margin-top:12px">
      <el-col :span="12" v-for="i in list" :key="i.exercise.id" style="margin-bottom:12px">
        <el-card shadow="hover">
          <div style="font-weight:600">{{ i.exercise.title }}</div>
          <div style="font-size:12px;color:#666;margin:8px 0">{{ i.reason }}</div>
          <el-button type="primary" plain @click="goPractice(i.exercise.id)">去做题</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
import router from '@/router'

const list = ref([])
const load = () => request.get('/api/student/extend/today', { params: { size: 8 } }).then(res => list.value = res.data || [])
const goPractice = (id) => router.push(`/student/practice?id=${id}`)
load()
</script>
