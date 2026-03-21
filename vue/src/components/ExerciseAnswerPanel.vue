<template>
  <div class="exercise-panel" v-if="q.id">
    <div class="panel-header">
      <div>
        <h3>{{ q.chapter }} · {{ displayQuestionNo }}</h3>
        <div class="knowledge-row" v-if="knowledgePoints.length">
          <el-tag v-for="kp in knowledgePoints" :key="kp" size="small" type="info" effect="plain">{{ kp }}</el-tag>
        </div>
      </div>
      <div class="action-row" v-if="embedded">
        <el-button text @click="$emit('close')">关闭</el-button>
      </div>
    </div>
    <p class="stem">{{ q.stem }}</p>
    <el-radio-group v-model="chosen" @change="submit" class="option-group">
      <el-radio v-for="k in ['A','B','C','D']" :key="k" :label="k" :class="wrong===k ? 'wrong' : ''">{{k}}. {{ q['option'+k] }}</el-radio>
    </el-radio-group>
    <el-alert v-if="feedback.visible" :type="feedback.type" :title="feedback.title" :description="feedback.description" show-icon :closable="false"/>
    <div class="actions">
      <el-button :disabled="!canGoPrev" @click="prevQuestion">上一题</el-button>
      <el-button @click="nextQuestion">下一题</el-button>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import request from '@/utils/request'
import {ElMessage} from 'element-plus'
import {parseKnowledgePoints} from '@/utils/knowledge'

const props = defineProps({
  initialId: { type: String, default: '' },
  sequenceIds: { type: Array, default: () => [] },
  embedded: { type: Boolean, default: false },
  randomWhenEmpty: { type: Boolean, default: true }
})

const emit = defineEmits(['close'])
const q = reactive({})
const chosen = ref('')
const wrong = ref('')
const history = ref([])
const historyIndex = ref(-1)
const sequenceIds = ref([])
const feedback = reactive({ visible: false, type: 'info', title: '', description: '' })

const knowledgePoints = computed(() => parseKnowledgePoints(q.knowledgePoints))
const currentSequenceIndex = computed(() => sequenceIds.value.indexOf(q.id))
const canGoPrev = computed(() => historyIndex.value > 0 || currentSequenceIndex.value > 0)

const displayQuestionNo = computed(() => {
  const id = String(q.id || '')
  const matched = id.match(/(\d{4,})$/)
  return matched ? matched[1] : id
})

const resetFeedback = () => {
  feedback.visible = false
  feedback.type = 'info'
  feedback.title = ''
  feedback.description = ''
}

const resetState = () => {
  chosen.value = ''
  wrong.value = ''
  history.value = []
  historyIndex.value = -1
  resetFeedback()
}

const fillQuestion = (item) => {
  Object.keys(q).forEach(k => delete q[k])
  Object.assign(q, item || {})
}

const appendHistory = (item) => {
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  history.value.push(item)
  historyIndex.value = history.value.length - 1
}

const prependHistory = (item) => {
  const current = history.value[historyIndex.value] || null
  history.value = current ? [item, current, ...history.value.slice(historyIndex.value + 1)] : [item, ...history.value]
  historyIndex.value = 0
}

const resetChoiceState = () => {
  chosen.value = ''
  wrong.value = ''
  resetFeedback()
}

const moveInHistory = (targetIndex) => {
  historyIndex.value = targetIndex
  resetChoiceState()
  fillQuestion(history.value[targetIndex])
}

const loadById = (id, historyMode = 'append') => request.get('/api/exercises/' + id).then(res => {
  const item = res.data || {}
  fillQuestion(item)
  if (historyMode === 'prepend') {
    prependHistory(item)
  } else if (historyMode !== 'skip') {
    appendHistory(item)
  }
  resetChoiceState()
})

const nextRandom = () => {
  chosen.value = ''
  wrong.value = ''
  resetFeedback()
  request.get('/api/exercises/random').then(res => {
    const item = res.data || {}
    fillQuestion(item)
    appendHistory(item)
  })
}

const nextBySequence = () => {
  const currentIdx = sequenceIds.value.indexOf(q.id)
  const nextId = currentIdx >= 0 ? sequenceIds.value[currentIdx + 1] : null
  if (!nextId) {
    ElMessage.info('已经是该列表最后一题')
    return
  }
  loadById(nextId)
}

const nextQuestion = () => {
  if (historyIndex.value < history.value.length - 1) {
    moveInHistory(historyIndex.value + 1)
    return
  }
  if (sequenceIds.value.length > 0) {
    nextBySequence()
    return
  }
  if (props.randomWhenEmpty) {
    nextRandom()
    return
  }
  ElMessage.info('当前没有更多题目了')
}

const prevQuestion = () => {
  if (historyIndex.value > 0) {
    moveInHistory(historyIndex.value - 1)
    return
  }
  if (currentSequenceIndex.value > 0) {
    loadById(sequenceIds.value[currentSequenceIndex.value - 1], 'prepend')
  }
}

const submit = () => {
  request.post('/api/answers/submit', { exerciseId: q.id, chosenOption: chosen.value }).then(res => {
    if (res.data.isCorrect) {
      feedback.visible = true
      feedback.type = 'success'
      feedback.title = '回答正确'
      feedback.description = res.data.analysis || '系统已为你自动切换到下一题。'
      ElMessage.success('回答正确，自动下一题')
      setTimeout(() => nextQuestion(), props.embedded ? 500 : 300)
    } else {
      wrong.value = chosen.value
      feedback.visible = true
      feedback.type = 'error'
      feedback.title = `回答错误，正确答案：${res.data.correctAnswer || '-'}`
      feedback.description = res.data.analysis || '请根据解析继续巩固该知识点。'
      ElMessage.error('错误，请继续作答')
    }
  })
}

const init = () => {
  resetState()
  sequenceIds.value = Array.isArray(props.sequenceIds) ? props.sequenceIds.filter(Boolean) : []
  if (props.initialId) {
    loadById(props.initialId)
    return
  }
  if (sequenceIds.value.length > 0) {
    loadById(sequenceIds.value[0])
    return
  }
  if (props.randomWhenEmpty) {
    nextRandom()
  }
}

onMounted(init)
watch(() => [props.initialId, JSON.stringify(props.sequenceIds), props.randomWhenEmpty], init)
</script>

<style scoped>
.exercise-panel{background:#fff;padding:18px;border-radius:12px;display:flex;flex-direction:column;gap:12px}
.panel-header{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}
.panel-header h3{margin:0}
.knowledge-row,.action-row,.actions{display:flex;gap:8px;flex-wrap:wrap}
.stem{line-height:1.8;margin:0}
.option-group{display:flex;flex-direction:column;gap:8px;align-items:stretch}
.option-group :deep(.el-radio){display:flex;align-items:flex-start;justify-content:flex-start;margin-right:0;text-align:left;white-space:normal}
.option-group :deep(.el-radio__input){margin-top:3px}
.option-group :deep(.el-radio__label){display:block;white-space:normal;line-height:1.8;padding-left:8px;text-align:left}
.wrong{color:#f56c6c}
</style>
