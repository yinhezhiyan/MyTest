<template>
  <div class="card">
    <el-input-number v-model="exerciseId" :min="1" />
    <el-button @click="load">加载题目</el-button>
    <div v-if="exercise" style="margin-top:10px">
      <h4>{{ exercise.title }}</h4>
      <div>{{ exercise.content }}</div>
      <el-radio-group v-model="isCorrect" style="margin-top:10px">
        <el-radio :label="1">答对</el-radio>
        <el-radio :label="0">答错</el-radio>
      </el-radio-group>
      <div><el-input v-model="answer" type="textarea" placeholder="可选：输入你的答案"/></div>
      <el-button type="primary" @click="submit">提交结果</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
const route = useRoute()
const exerciseId = ref(Number(route.query.id || 1))
const exercise = ref(null)
const isCorrect = ref(1)
const answer = ref('')
const load = () => request.get(`/api/student/exercises/${exerciseId.value}`).then(res => exercise.value = res.data)
const submit = () => {
  request.post('/api/student/behaviors/answer', { exerciseId: exerciseId.value, isCorrect: isCorrect.value, answer: answer.value }).then(res => {
    if (res.code === '200') ElMessage.success('提交成功')
  })
}
load()
</script>
