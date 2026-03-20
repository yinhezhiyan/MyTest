<template>
  <div class="card">
    <template v-if="answering">
      <div class="answer-header">
        <div>
          <h2>今日个性拓展作答</h2>
          <div class="subline">当前页面已切换到今日拓展题目的连续作答模式。</div>
        </div>
        <el-button @click="closeAnswer">返回拓展列表</el-button>
      </div>
      <ExerciseAnswerPanel
        :key="answerKey"
        :initial-id="activeId"
        :sequence-ids="sequenceIds"
        :embedded="true"
        :random-when-empty="false"
        @close="closeAnswer"
      />
    </template>

    <template v-else>
      <h2>今日个性拓展</h2>
      <el-empty v-if="!list.length" description="暂无推荐"/>
      <el-timeline v-else>
        <el-timeline-item v-for="item in list" :key="item.exerciseId">
          <div class="item-title">{{ item.stem }}</div>
          <div class="meta-row">
            <el-tag size="small" type="success">{{ Number(item.score || 0).toFixed(2) }}</el-tag>
            <span class="reason-text">{{ item.reason }}</span>
          </div>
          <el-button link type="primary" @click="openAnswer(item.exerciseId)">开始作答</el-button>
        </el-timeline-item>
      </el-timeline>
    </template>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue'
import request from '@/utils/request'
import ExerciseAnswerPanel from '@/components/ExerciseAnswerPanel.vue'

const list = ref([])
const answering = ref(false)
const activeId = ref('')
const sequenceIds = ref([])
const answerKey = ref(0)

const load = () => request.get('/api/daily').then(res => list.value = res.data || [])
const openAnswer = (id) => {
  activeId.value = id
  sequenceIds.value = list.value.map(item => item.exerciseId)
  answerKey.value += 1
  answering.value = true
}
const closeAnswer = () => {
  answering.value = false
}

onMounted(load)
</script>
<style scoped>
.card{background:#fff;padding:18px;border-radius:12px}
.answer-header{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:12px}
.item-title{font-weight:600}
.meta-row{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin:6px 0;color:#64748b}
.reason-text,.subline{line-height:1.6;color:#64748b}
</style>
