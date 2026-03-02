<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size: 30px; text-align: center; margin-bottom: 30px; color: #1967e3">欢 迎 登 录</div>
      <el-form :model="data.form" ref="formRef" :rules="data.rules">
        <el-form-item prop="username"><el-input :prefix-icon="User" size="large" v-model="data.form.username" placeholder="请输入账号" /></el-form-item>
        <el-form-item prop="password"><el-input :prefix-icon="Lock" size="large" v-model="data.form.password" placeholder="请输入密码" show-password /></el-form-item>
        <el-form-item prop="loginType">
          <el-select size="large" style="width: 100%" v-model="data.form.loginType">
            <el-option value="ADMIN" label="管理员"/>
            <el-option value="STUDENT" label="学生"/>
          </el-select>
        </el-form-item>
        <el-form-item prop="subject">
          <el-select size="large" style="width: 100%" v-model="data.form.subject" placeholder="请选择学科">
            <el-option label="数学" value="数学"/>
            <el-option label="英语" value="英语"/>
            <el-option label="C语言程序设计" value="C语言程序设计"/>
          </el-select>
        </el-form-item>
        <el-form-item><el-button size="large" type="primary" style="width: 100%" @click="login">登 录</el-button></el-form-item>
      </el-form>
      <div style="text-align: right;">还没有账号？请 <a href="/register">注册</a>
        <div style="margin-top:8px;color:#888">演示管理员：admin_math/admin123(数学)</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import router from '@/router'

const data = reactive({
  form: { loginType: 'STUDENT', subject: '数学' },
  rules: {
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
  }
})
const formRef = ref()
const login = () => {
  formRef.value.validate(valid => {
    if (!valid) return
    request.post('/api/auth/login', data.form).then(res => {
      if (res.code === '200') {
        ElMessage.success('登录成功')
        localStorage.setItem('system-token', res.data.token)
        localStorage.setItem('system-user', JSON.stringify(res.data.user))
        const role = res.data.user?.role
        router.push(role === 'ADMIN' ? '/admin/home' : '/student/dashboard')
      } else ElMessage.error(res.msg)
    })
  })
}
</script>

<style scoped>
.login-container { height: 100vh; overflow:hidden; display: flex; justify-content: center; align-items: center; background: #2e3143; }
.login-box { width: 350px; padding: 40px 30px; border-radius: 5px; box-shadow: 0 0 10px rgba(255, 255, 255, 0.3); background-color: #fff; }
</style>
