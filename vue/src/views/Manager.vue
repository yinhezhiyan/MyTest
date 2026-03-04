<template>
  <div>
    <div class="topbar">
      <div class="brand"><img src="@/assets/imgs/logo.png" alt=""><span>个性化习题推荐系统</span></div>
      <div class="userbox">
        <el-tag>{{ subjectText }}</el-tag>
        <el-tag :type="data.user.role === 'ADMIN' ? 'danger' : 'success'">{{ data.user.role === 'ADMIN' ? '管理员' : '学生' }}</el-tag>
        <el-avatar :src="data.user.avatar" :size="34">{{ displayName.slice(0,1) }}</el-avatar>
        <span>{{ displayName }}</span>
        <el-button link style="color:#fff" @click="openProfile">个人设置</el-button>
      </div>
    </div>
    <div class="layout">
      <div class="sidebar">
        <el-menu router :default-active="router.currentRoute.value.path" style="border:none;background:transparent;">
          <el-menu-item :index="`/manager/${data.user.subject}/home`">首页</el-menu-item>
          <template v-if="data.user.role === 'ADMIN'">
            <el-menu-item :index="`/manager/${data.user.subject}/admin`">管理员</el-menu-item>
            <el-menu-item :index="`/manager/${data.user.subject}/students`">学生管理</el-menu-item>
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

    <el-dialog v-model="profileVisible" title="个人设置" width="420px">
      <el-form :model="profile" label-width="70px">
        <el-form-item label="头像">
          <el-upload :action="uploadUrl" list-type="picture" :on-success="handleImgSuccess">
            <el-button type="primary">上传图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profile.name" placeholder="请输入昵称"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible=false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, reactive, ref} from "vue";
import router from "@/router";
import request from "@/utils/request";
import {ElMessage} from "element-plus";

const uploadUrl = import.meta.env.VITE_BASE_URL + '/files/upload'
const data = reactive({ user: JSON.parse(localStorage.getItem('system-user') || '{}') })
const profileVisible = ref(false)
const profile = reactive({ name: '', avatar: '' })
const displayName = computed(() => data.user.name || data.user.username || '用户')
const subjectText = computed(() => ({DS: '数据结构', OS: '操作系统', CN: '计网', CO: '计组'})[data.user.subject] || data.user.subject)

const syncUser = (nextUser) => {
  data.user = nextUser
  localStorage.setItem('system-user', JSON.stringify(nextUser))
  window.dispatchEvent(new CustomEvent('user-profile-updated', { detail: nextUser }))
}

const openProfile = () => {
  request.get('/account/profile').then(res => {
    Object.assign(profile, res.data || {})
    profileVisible.value = true
  })
}

const handleImgSuccess = (res) => {
  profile.avatar = res.data
  const nextUser = { ...data.user, avatar: res.data }
  syncUser(nextUser)
}

const saveProfile = () => {
  request.put('/account/profile', { name: profile.name, avatar: profile.avatar }).then(res => {
    if (res.code === '200') {
      const currentToken = data.user.token
      const nextUser = { ...data.user, ...res.data, token: res.data?.token || currentToken }
      syncUser(nextUser)
      profileVisible.value = false
      ElMessage.success('保存成功')
    } else ElMessage.error(res.msg)
  })
}

const logout = () => {
  localStorage.removeItem('system-user')
  window.dispatchEvent(new CustomEvent('user-profile-updated', { detail: {} }))
  router.push('/login')
}
</script>

<style scoped>
.topbar{height:60px;background:#1f2a44;color:#fff;display:flex;justify-content:space-between;align-items:center;padding:0 20px}
.brand{display:flex;align-items:center;gap:8px;font-weight:700}.brand img{width:34px}
.userbox{display:flex;align-items:center;gap:8px}
.layout{display:flex;height:calc(100vh - 60px);background:#f4f7fb}
.sidebar{width:220px;background:#fff;border-right:1px solid #e5e7eb;padding-top:12px}
.content{flex:1;padding:16px;overflow:auto}
:deep(.el-menu-item.is-active){
  background:#e7eefc !important;
  color:#1f2a44 !important;
  font-weight:700;
}
</style>
