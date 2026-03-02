<template>
  <div>
    <div style="height:60px;background:#2e3143;color:#fff;display:flex;align-items:center;padding:0 16px;justify-content:space-between">
      <div style="font-weight:700">管理员后台</div>
      <div>{{ user.name || user.username }} <el-button link style="color:#fff" @click="logout">退出</el-button></div>
    </div>
    <div style="display:flex;min-height:calc(100vh - 60px)">
      <el-menu router :default-active="router.currentRoute.value.path" style="width:220px">
        <el-menu-item index="/admin/home">首页</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/exercises">题库管理</el-menu-item>
        <el-menu-item index="/admin/knowledge">知识点管理</el-menu-item>
        <el-menu-item index="/admin/recommend-logs">推荐日志</el-menu-item>
      </el-menu>
      <div style="flex:1;padding:12px;background:#f8f8ff"><router-view /></div>
    </div>
  </div>
</template>
<script setup>
import router from '@/router'
const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const logout = () => {
  localStorage.removeItem('system-token')
  localStorage.removeItem('system-user')
  router.push('/login')
}
</script>
