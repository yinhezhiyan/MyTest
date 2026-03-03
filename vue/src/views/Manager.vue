<template>
  <div>
    <div class="topbar">
      <div class="brand"><img src="@/assets/imgs/logo.png" alt=""><span>个性化习题推荐系统</span></div>
      <div class="userbox">
        <el-tag>{{ subjectText }}</el-tag>
        <el-tag :type="data.user.role === 'ADMIN' ? 'danger' : 'success'">{{ data.user.role === 'ADMIN' ? '管理员' : '学生' }}</el-tag>
        <span>{{ data.user.name }}</span>
      </div>
    </div>
    <div class="layout">
      <div class="sidebar">
        <el-menu router :default-active="router.currentRoute.value.path" style="border:none;background:transparent;">
          <el-menu-item :index="`/manager/${data.user.subject}/home`">首页</el-menu-item>
          <template v-if="data.user.role === 'ADMIN'">
            <el-menu-item :index="`/manager/${data.user.subject}/admin`">管理员</el-menu-item>
          </template>
          <template v-else>
            <el-menu-item :index="`/manager/${data.user.subject}/daily`">今日个性拓展</el-menu-item>
            <el-menu-item :index="`/manager/${data.user.subject}/practice`">开始练习</el-menu-item>
            <el-menu-item :index="`/manager/${data.user.subject}/recommend`">习题推荐</el-menu-item>
            <el-menu-item :index="`/manager/${data.user.subject}/records`">答题记录</el-menu-item>
          </template>
          <el-menu-item @click="logout">退出登录</el-menu-item>
        </el-menu>
      </div>
      <div class="content"><router-view /></div>
    </div>
  </div>
</template>

<script setup>
import {computed, reactive} from "vue";
import router from "@/router";

const data = reactive({ user: JSON.parse(localStorage.getItem('system-user') || '{}') })
const subjectText = computed(() => ({DS: '数据结构', OS: '操作系统', CN: '计网', CO: '计组'})[data.user.subject] || data.user.subject)
const logout = () => { localStorage.removeItem('system-user'); router.push('/login') }
</script>

<style scoped>
.topbar{height:60px;background:#1f2a44;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:0 20px}
.brand{display:flex;align-items:center;gap:8px;font-weight:700}.brand img{width:34px}
.userbox{display:flex;align-items:center;gap:8px}
.layout{display:flex;height:calc(100vh - 60px);background:#f4f7fb}
.sidebar{width:220px;background:#fff;border-right:1px solid #e5e7eb;padding-top:12px}
.content{flex:1;padding:16px;overflow:auto}
</style>
