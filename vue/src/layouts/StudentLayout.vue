<template>
  <div>
    <div style="height:60px;background:#1f3c88;color:#fff;display:flex;align-items:center;padding:0 16px;justify-content:space-between">
      <div style="font-weight:700">学生学习端</div>
      <div>{{ user.name || user.username }}（{{ user.subject }}） <el-button link style="color:#fff" @click="logout">退出</el-button></div>
    </div>
    <div style="padding:10px;background:#eef3ff;display:flex;gap:10px">
      <el-button @click="go('/student/dashboard')">Dashboard</el-button>
      <el-button @click="go('/student/recommend')">Recommend</el-button>
      <el-button @click="go('/student/practice')">Practice</el-button>
      <el-button @click="go('/student/records')">Records</el-button>
    </div>
    <div style="padding:12px"><router-view /></div>
  </div>
</template>
<script setup>
import router from '@/router'
const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const go = (path) => router.push(path)
const logout = () => {
  localStorage.removeItem('system-token')
  localStorage.removeItem('system-user')
  router.push('/login')
}
</script>
