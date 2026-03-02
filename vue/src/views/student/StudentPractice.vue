<template>
  <div class="card">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <h3>开始练习</h3>
      <el-button @click="loadNext">换一题</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <div v-else-if="exercise">
      <h4>{{ exercise.title }}</h4>
      <p>{{ exercise.content }}</p>

      <div class="options">
        <button
          v-for="opt in options"
          :key="opt.key"
          class="opt"
          :class="{ wrong: wrongOption === opt.key, right: correctOption === opt.key }"
          @click="choose(opt.key)"
          :disabled="submitting"
        >
          {{ opt.key }}. {{ opt.value }}
        </button>
      </div>

      <div v-if="message" style="margin-top:10px;color:#666">{{ message }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const exercise = ref(null)
const wrongOption = ref('')
const correctOption = ref('')
const message = ref('')

const options = computed(() => {
  if (!exercise.value) return []
  return [
    { key: 'A', value: exercise.value.optionA || '选项A' },
    { key: 'B', value: exercise.value.optionB || '选项B' },
    { key: 'C', value: exercise.value.optionC || '选项C' },
    { key: 'D', value: exercise.value.optionD || '选项D' }
  ]
})

const loadById = (id) => request.get(`/api/student/exercises/${id}`).then(res => {
  exercise.value = res.data
  wrongOption.value = ''
  correctOption.value = ''
  message.value = ''
})

const loadNext = () => {
  loading.value = true
  const id = route.query.id
  const api = id ? loadById(id) : request.get('/api/student/practice/next').then(res => { exercise.value = res.data })
  api.then(() => {
    wrongOption.value = ''
    correctOption.value = ''
    message.value = ''
  }).finally(() => loading.value = false)
}

const choose = (picked) => {
  if (!exercise.value) return
  submitting.value = true
  request.post('/api/student/attempt', { exerciseId: exercise.value.id, chosen: picked }).then(res => {
    const ok = !!res.data?.isCorrect
    if (ok) {
      correctOption.value = picked
      wrongOption.value = ''
      message.value = '回答正确，正在进入下一题...'
      setTimeout(loadNext, 600)
    } else {
      wrongOption.value = picked
      correctOption.value = ''
      message.value = `回答错误，请重试。解析：${res.data?.analysis || '暂无'}`
    }
  }).catch(() => ElMessage.error('提交失败')).finally(() => submitting.value = false)
}

loadNext()
</script>

<style scoped>
.options { display: grid; gap: 10px; margin-top: 12px; }
.opt { border: 1px solid #dcdfe6; background:#fff; padding:10px 12px; border-radius:8px; text-align:left; cursor:pointer; }
.opt:hover { box-shadow: 0 2px 10px rgba(0,0,0,.06); }
.opt.wrong { border-color:#f56c6c; background:#fef0f0; }
.opt.right { border-color:#67c23a; background:#f0f9eb; }
</style>
