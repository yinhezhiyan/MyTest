<template>
  <div>
    <div style="height:64px;background:linear-gradient(90deg,#1f3c88,#2959d4);color:#fff;display:flex;align-items:center;padding:0 18px;justify-content:space-between">
      <div style="font-weight:700">个性化习题推荐学生端</div>
      <div>{{ user.name || user.username }}（{{ user.subject }}） <el-button link style="color:#fff" @click="logout">退出</el-button></div>
    </div>
    <div style="padding:10px;background:#eef3ff;display:flex;gap:10px;flex-wrap:wrap">
      <el-button @click="go('/student/dashboard')">首页</el-button>
      <el-button @click="go('/student/extend')">个性拓展</el-button>
      <el-button @click="go('/student/practice')">开始练习</el-button>
      <el-button @click="go('/student/recommend')">习题推荐</el-button>
      <el-button @click="go('/student/records')">答题记录</el-button>
    </div>
    <div style="padding:14px"><router-view /></div>
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
