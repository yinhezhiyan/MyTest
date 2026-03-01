<template>
  <div>
    <div style="height: 60px; background-color: #2e3143; display: flex; align-items: center; border-bottom: 1px solid #ddd">
      <div style="flex: 1">
        <div style="padding-left: 20px; display: flex; align-items: center">
          <img src="@/assets/imgs/logo.png" alt="" style="width: 40px">
          <div style="font-weight: bold; font-size: 24px; margin-left: 5px; color: #fff">个性化习题推荐系统（演示版）</div>
        </div>
      </div>
      <div style="width: fit-content; padding-right: 10px; display: flex; align-items: center;">
        <img style="width: 40px; height: 40px; border-radius: 50%" :src="data.user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="">
        <span style="color: #fff; margin-left: 5px">{{ data.user.name || '未登录' }}</span>
      </div>
    </div>

    <div style="display: flex">
      <div style="width: 220px; border-right: 1px solid #ddd; min-height: calc(100vh - 60px)">
        <el-menu router style="border: none" :default-active="router.currentRoute.value.path" :default-openeds="['user']">
          <el-menu-item index="/manager/home"><el-icon><HomeFilled /></el-icon><span>系统首页</span></el-menu-item>

          <template v-if="isAdmin">
            <el-sub-menu index="user">
              <template #title><el-icon><User /></el-icon><span>用户管理</span></template>
              <el-menu-item index="/manager/admin"><el-icon><User /></el-icon><span>管理员信息</span></el-menu-item>
              <el-menu-item index="/manager/users"><el-icon><User /></el-icon><span>学生用户</span></el-menu-item>
              <el-menu-item index="/manager/exercises"><el-icon><Document /></el-icon><span>题库管理</span></el-menu-item>
              <el-menu-item index="/manager/knowledge"><el-icon><Collection /></el-icon><span>知识点管理</span></el-menu-item>
              <el-menu-item index="/manager/recommend-logs"><el-icon><DataLine /></el-icon><span>推荐日志</span></el-menu-item>
            </el-sub-menu>
          </template>

          <el-menu-item index="/manager/recommend"><el-icon><MagicStick /></el-icon><span>我的推荐</span></el-menu-item>
          <el-menu-item @click="logout"><el-icon><SwitchButton /></el-icon><span>退出系统</span></el-menu-item>
        </el-menu>
      </div>
      <div style="flex: 1; width: 0; background-color: #f8f8ff; padding: 10px">
        <router-view @updateUser="updateUser" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import router from '@/router'
import { ElMessage } from 'element-plus'

const data = reactive({ user: JSON.parse(localStorage.getItem('system-user') || '{}') })
const isAdmin = computed(() => data.user?.role === 'ADMIN')

if (!data.user?.id || !localStorage.getItem('system-token')) {
  ElMessage.error('请先登录')
  router.push('/login')
}

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem('system-user') || '{}')
}

const logout = () => {
  localStorage.removeItem('system-user')
  localStorage.removeItem('system-token')
  ElMessage.success('退出成功')
  router.push('/login')
}
</script>

<style scoped>
.el-menu-item.is-active { background-color: #d7d7e6 !important; }
.el-menu-item:hover { color: #000; }
</style>
