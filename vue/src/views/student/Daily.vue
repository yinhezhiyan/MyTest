<template>
  <div class="card">
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

    <el-dialog v-model="answerVisible" title="个性化拓展作答" width="70%" top="6vh" destroy-on-close>
      <ExerciseAnswerPanel
        v-if="answerVisible"
        :key="dialogKey"
        :initial-id="activeId"
        :sequence-ids="sequenceIds"
        :embedded="true"
        :random-when-empty="false"
        @close="answerVisible = false"
      />
    </el-dialog>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue'
import request from '@/utils/request'
import ExerciseAnswerPanel from '@/components/ExerciseAnswerPanel.vue'

const list = ref([])
const answerVisible = ref(false)
const activeId = ref('')
const sequenceIds = ref([])
const dialogKey = ref(0)

const load = () => request.get('/api/daily').then(res => list.value = res.data || [])
const openAnswer = (id) => {
  activeId.value = id
  sequenceIds.value = list.value.map(item => item.exerciseId)
  dialogKey.value += 1
  answerVisible.value = true
}

onMounted(load)
</script>
<style scoped>
.card{background:#fff;padding:18px;border-radius:12px}
.item-title{font-weight:600}
.meta-row{display:flex;gap:8px;align-items:center;flex-wrap:wrap;margin:6px 0;color:#64748b}
.reason-text{line-height:1.6}
</style>
