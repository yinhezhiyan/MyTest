<template>
  <div class="login-container">
    <div class="login-box">
      <div style="font-weight: bold; font-size: 28px; text-align: center; margin-bottom: 20px; color: #1967e3">学 生 注 册</div>
      <el-form :model="form" ref="formRef" :rules="rules">
        <el-form-item prop="username"><el-input v-model="form.username" placeholder="请输入账号" /></el-form-item>
        <el-form-item prop="name"><el-input v-model="form.name" placeholder="请输入姓名" /></el-form-item>
        <el-form-item prop="password"><el-input v-model="form.password" show-password placeholder="请输入密码" /></el-form-item>
        <el-form-item prop="subject">
          <el-select v-model="form.subject" placeholder="请选择学科" style="width:100%">
            <el-option label="数学" value="数学"/>
            <el-option label="英语" value="英语"/>
            <el-option label="C语言程序设计" value="C语言程序设计"/>
          </el-select>
        </el-form-item>
        <el-form-item prop="grade"><el-input v-model="form.grade" placeholder="请输入年级（如大二）" /></el-form-item>
        <el-form-item><el-button type="primary" style="width:100%" @click="register">注 册</el-button></el-form-item>
      </el-form>
      <div style="text-align: right;">已有账号？<a href="/login">去登录</a></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import router from '@/router'

const formRef = ref()
const form = reactive({ role: 'STUDENT' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  subject: [{ required: true, message: '请选择学科', trigger: 'change' }]
}

const register = () => {
  formRef.value.validate(valid => {
    if (!valid) return
    request.post('/api/auth/register', form).then(res => {
      if (res.code === '200') {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}
</script>

<style scoped>
.login-container { height:100vh; display:flex; justify-content:center; align-items:center; background:#2e3143; }
.login-box { width:380px; padding:40px 30px; border-radius:6px; background:#fff; box-shadow:0 0 10px rgba(255,255,255,.3); }
</style>
