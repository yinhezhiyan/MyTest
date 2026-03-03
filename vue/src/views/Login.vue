<template>
  <div class="login-container">
    <div class="login-box">
      <div class="title">课程学习平台</div>
      <div class="subtitle">按身份与学科进入对应空间</div>
      <el-form :model="data.form" ref="formRef" :rules="data.rules">
        <el-form-item prop="role">
          <el-radio-group v-model="data.form.role" size="large" style="width: 100%">
            <el-radio-button label="ADMIN">管理员</el-radio-button>
            <el-radio-button label="STUDENT">学生</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item prop="subject">
          <el-select size="large" style="width: 100%" v-model="data.form.subject" placeholder="请选择科目">
            <el-option v-for="item in data.subjects" :key="item.value" :value="item.value" :label="item.label"/>
          </el-select>
        </el-form-item>
        <el-form-item prop="username">
          <el-input :prefix-icon="User" size="large" v-model="data.form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input :prefix-icon="Lock" size="large" v-model="data.form.password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button size="large" type="primary" class="full-btn" @click="login">登 录</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align: right;" v-if="data.form.role === 'STUDENT'">
        还没有账号？请 <a href="javascript:void(0)" @click="data.registerVisible = true">注册</a>
      </div>
      <div style="text-align: right;" v-else>
        管理员账号由系统内管理员创建
      </div>
    </div>

    <el-dialog v-model="data.registerVisible" title="学生注册" width="420px" destroy-on-close>
      <el-form :model="data.registerForm" :rules="data.registerRules" ref="registerRef" label-width="70px">
        <el-form-item prop="subject" label="科目">
          <el-select v-model="data.registerForm.subject" style="width: 100%" placeholder="请选择科目">
            <el-option v-for="item in data.subjects" :key="item.value" :value="item.value" :label="item.label"/>
          </el-select>
        </el-form-item>
        <el-form-item prop="username" label="账号">
          <el-input v-model="data.registerForm.username"/>
        </el-form-item>
        <el-form-item prop="name" label="姓名">
          <el-input v-model="data.registerForm.name"/>
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input show-password v-model="data.registerForm.password"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.registerVisible = false">取消</el-button>
        <el-button type="primary" @click="register">提交注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import request from "@/utils/request";
import {ElMessage} from "element-plus";
import router from "@/router";

const data = reactive({
  form: { role: 'ADMIN', subject: 'MATH' },
  registerVisible: false,
  registerForm: { role: 'STUDENT', subject: 'MATH' },
  subjects: [
    { label: '数学', value: 'MATH' },
    { label: '操作系统', value: 'OS' },
    { label: '数据结构', value: 'DS' },
  ],
  rules: {
    role: [{ required: true, message: '请选择身份', trigger: 'change' }],
    subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  },
  registerRules: {
    subject: [{ required: true, message: '请选择科目', trigger: 'change' }],
    username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  }
})

const formRef = ref()
const registerRef = ref()

const login = () => {
  formRef.value.validate((valid => {
    if (valid) {
      request.post('/login', data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success("登录成功")
          localStorage.setItem('system-user', JSON.stringify(res.data))
          router.push(`/manager/${res.data.subject}/home`)
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  }))
}

const register = () => {
  registerRef.value.validate((valid => {
    if (valid) {
      request.post('/register', {...data.registerForm, role: 'STUDENT'}).then(res => {
        if (res.code === '200') {
          ElMessage.success('注册成功，请登录')
          data.registerVisible = false
          data.form.role = 'STUDENT'
          data.form.subject = data.registerForm.subject
          data.form.username = data.registerForm.username
          data.form.password = ''
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  }))
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  overflow:hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #2e3143 0%, #434a73 100%);
}
.login-box {
  width: 380px;
  padding: 40px 30px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.22);
  background-color: #fff;
}
.title {
  font-weight: bold;
  font-size: 32px;
  text-align: center;
  margin-bottom: 8px;
  color: #1967e3;
}
.subtitle {
  font-size: 14px;
  text-align: center;
  color: #6b7280;
  margin-bottom: 25px;
}
.full-btn {
  width: 100%;
}
</style>
