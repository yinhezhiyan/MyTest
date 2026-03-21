<template>
  <div class="card">
    <template v-if="answering">
      <div class="answer-header">
        <div>
          <h2>习题推荐作答</h2>
          <div class="subline">当前页面已切换到推荐题目的连续作答模式。</div>
        </div>
        <el-button @click="closeAnswer">返回推荐列表</el-button>
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
      <div class="header-row">
        <h2>习题推荐</h2>
        <el-dropdown trigger="click">
          <el-button circle><el-icon><MoreFilled /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click.stop>
                <div class="setting-item">
                  <span>包含已做过题目</span>
                  <el-switch v-model="includeDone" @change="load"/>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <el-table :data="list">
        <el-table-column prop="chapter" label="章节" width="160"/>
        <el-table-column prop="stem" label="题目"/>
        <el-table-column label="推荐说明" min-width="280">
          <template #default="scope">
            <div class="reason-wrap">
              <div class="score-line">
                <el-tag type="success">推荐分 {{ Number(scope.row.score || 0).toFixed(2) }}</el-tag>
                <el-tag v-if="scope.row.difficulty">难度 {{ scope.row.difficulty }}</el-tag>
              </div>
              <div class="reason-text">{{ scope.row.reason || '系统综合推荐' }}</div>
              <div class="tag-row">
                <el-tag v-for="tag in scope.row.reasonTags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
                <el-tag v-for="kp in sanitizeKnowledgePoints(scope.row.knowledgePoints)" :key="`${scope.row.exerciseId}-${kp}`" size="small" type="info" effect="plain">{{ kp }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope"><el-button type="primary" link @click="openAnswer(scope.row.exerciseId)">去作答</el-button></template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue'
import {MoreFilled} from '@element-plus/icons-vue'
import request from '@/utils/request'
import ExerciseAnswerPanel from '@/components/ExerciseAnswerPanel.vue'
import {sanitizeKnowledgePoints} from '@/utils/knowledge'

const includeDone = ref(localStorage.getItem('recommend-include-done') === '1')
const list = ref([])
const answering = ref(false)
const activeId = ref('')
const sequenceIds = ref([])
const answerKey = ref(0)

const load = () => {
  localStorage.setItem('recommend-include-done', includeDone.value ? '1' : '0')
  request.get('/api/recommendations', { params: { topN: 15, includeDone: includeDone.value } }).then(res => list.value = res.data || [])
}

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
.header-row,.answer-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;gap:12px;flex-wrap:wrap}
.setting-item{display:flex;align-items:center;justify-content:space-between;gap:12px;width:220px}
.reason-wrap{display:flex;flex-direction:column;gap:8px;padding:6px 0}
.score-line,.tag-row{display:flex;gap:6px;flex-wrap:wrap}
.reason-text,.subline{color:#475569;line-height:1.6}
</style>
