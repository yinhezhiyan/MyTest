<template>
  <div>
    <div style="height: 60px; background-color: #2e3143; display: flex; align-items: center; border-bottom: 1px solid #ddd">
      <div style="flex: 1">
        <div style="padding-left: 20px; display: flex; align-items: center">
          <img src="@/assets/imgs/logo.png" alt="" style="width: 40px">
          <div style="font-weight: bold; font-size: 24px; margin-left: 5px; color: #fff">小白做毕设2026</div>
        </div>
      </div>
      <div style="width: fit-content; padding-right: 10px; display: flex; align-items: center; color: #fff; gap: 8px;">
        <el-tag size="small" type="info">{{ subjectText }}</el-tag>
        <el-tag size="small" :type="data.user.role === 'ADMIN' ? 'danger' : 'success'">{{ data.user.role === 'ADMIN' ? '管理员' : '学生' }}</el-tag>
        <img style="width: 40px; height: 40px; border-radius: 50%" :src="data.user.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="">
        <span>{{ data.user.name || '代码小白' }}</span>
      </div>
    </div>

    <div style="display: flex">
      <div style="width: 200px; border-right: 1px solid #ddd; min-height: calc(100vh - 60px)">
        <el-menu
            router
            style="border: none"
            :default-active="router.currentRoute.value.path"
            :default-openeds="['user']"
        >
          <el-menu-item :index="`/manager/${data.user.subject}/home`">
            <el-icon><HomeFilled /></el-icon>
            <span>系统首页</span>
          </el-menu-item>
          <el-sub-menu index="user" v-if="data.user.role === 'ADMIN'">
            <template #title>
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </template>
            <el-menu-item :index="`/manager/${data.user.subject}/admin`">
              <el-icon><User /></el-icon>
              <span>管理员信息</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item @click="logout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出系统</span>
          </el-menu-item>
        </el-menu>
      </div>
      <div style="flex: 1; width: 0; background-color: #f8f8ff; padding: 10px">
        <router-view @updateUser="updateUser" />
      </div>
    </div>

  </div>
</template>

<script setup>
import {computed, reactive} from "vue";
import router from "@/router";
import {ElMessage} from "element-plus";
import {useRoute} from "vue-router";

const route = useRoute()

const data = reactive({
  user: JSON.parse(localStorage.getItem('system-user') || '{}')
})

if (!data.user?.id || !data.user?.subject || !data.user?.token) {
  ElMessage.error('请登录！')
  router.push('/login')
}

if (route.params.subject && route.params.subject !== data.user.subject) {
  ElMessage.error('不可跨学科访问')
  router.push(`/manager/${data.user.subject}/home`)
}

const subjectText = computed(() => ({MATH: '数学', OS: '操作系统', DS: '数据结构'})[data.user.subject] || data.user.subject)

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem('system-user') || '{}')
}

const logout = () => {
  router.push('/login')
  ElMessage.success('退出成功')
  localStorage.removeItem('system-user')
}
</script>

<style scoped>
.el-menu-item.is-active {
  background-color: #d7d7e6 !important;
}
.el-menu-item:hover {
  color: #000;
}
</style>
