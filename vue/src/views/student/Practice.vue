<template>
  <div class="card" v-if="q.id">
    <h3>{{ q.chapter }} · {{ displayQuestionNo }}</h3>
    <p>{{ q.stem }}</p>
    <el-radio-group v-model="chosen" @change="submit">
      <el-radio v-for="k in ['A','B','C','D']" :key="k" :label="k" :class="wrong===k ? 'wrong' : ''">{{k}}. {{ q['option'+k] }}</el-radio>
    </el-radio-group>
    <div class="actions">
      <el-button :disabled="historyIndex <= 0" @click="prevQuestion">上一题</el-button>
      <el-button @click="nextQuestion">下一题</el-button>
    </div>
  </div>
</template>
<script setup>
import {computed, onMounted, reactive, ref} from 'vue';
import request from '@/utils/request';
import {ElMessage} from 'element-plus';
import {useRoute} from 'vue-router';
const route = useRoute();
const q = reactive({});
const chosen = ref('');
const wrong = ref('');
const history = ref([])
const historyIndex = ref(-1)
const sequenceIds = ref([])

const displayQuestionNo = computed(() => {
  const id = String(q.id || '')
  const matched = id.match(/(\d{4,})$/)
  return matched ? matched[1] : id
})

const fillQuestion = (item) => {
  Object.keys(q).forEach(k => delete q[k])
  Object.assign(q, item || {})
}

const loadById = (id)=> request.get('/api/exercises/'+id).then(res=> {
  const item = res.data || {}
  fillQuestion(item)
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  history.value.push(item)
  historyIndex.value = history.value.length - 1
})

const nextRandom = ()=>{
  chosen.value='';
  wrong.value='';
  request.get('/api/exercises/random').then(res=>{
    const item = res.data || {}
    fillQuestion(item)
    if (historyIndex.value < history.value.length - 1) {
      history.value = history.value.slice(0, historyIndex.value + 1)
    }
    history.value.push(item)
    historyIndex.value = history.value.length - 1
  })
}

const nextBySequence = () => {
  const currentIdx = sequenceIds.value.indexOf(q.id)
  const nextId = currentIdx >= 0 ? sequenceIds.value[currentIdx + 1] : null
  if (!nextId) {
    ElMessage.info('已经是该列表最后一题')
    return
  }
  chosen.value = ''
  wrong.value = ''
  loadById(nextId)
}

const nextQuestion = () => {
  if (sequenceIds.value.length > 0) {
    nextBySequence()
    return
  }
  nextRandom()
}

const prevQuestion = () => {
  if (historyIndex.value <= 0) return
  historyIndex.value -= 1
  chosen.value=''
  wrong.value=''
  fillQuestion(history.value[historyIndex.value])
}

const submit = ()=>{
  request.post('/api/answers/submit',{exerciseId:q.id, chosenOption:chosen.value}).then(res=>{
    if (res.data.isCorrect){ElMessage.success('回答正确，自动下一题');nextQuestion()} else {wrong.value=chosen.value;ElMessage.error('错误，请继续作答')}
  })
}

onMounted(()=> {
  if (route.query.ids) {
    sequenceIds.value = String(route.query.ids).split(',').filter(Boolean)
  }
  if (route.query.id) {
    loadById(route.query.id)
  } else if (sequenceIds.value.length > 0) {
    loadById(sequenceIds.value[0])
  } else {
    nextRandom()
  }
})
</script>
<style scoped>
.card{background:#fff;padding:18px;border-radius:12px}
.wrong{color:#f56c6c}
.actions{margin-top:12px;display:flex;gap:10px}
</style>
