<template>
  <div class="card">
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
              <el-tag v-for="kp in scope.row.knowledgePoints || []" :key="`${scope.row.exerciseId}-${kp}`" size="small" type="info" effect="plain">{{ kp }}</el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope"><el-button type="primary" link @click="openAnswer(scope.row.exerciseId)">去作答</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="answerVisible" title="推荐题目作答" width="70%" top="6vh" destroy-on-close>
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
import {MoreFilled} from '@element-plus/icons-vue'
import request from '@/utils/request'
import ExerciseAnswerPanel from '@/components/ExerciseAnswerPanel.vue'

const includeDone = ref(localStorage.getItem('recommend-include-done') === '1')
const list = ref([])
const answerVisible = ref(false)
const activeId = ref('')
const sequenceIds = ref([])
const dialogKey = ref(0)

const load = () => {
  localStorage.setItem('recommend-include-done', includeDone.value ? '1' : '0')
  request.get('/api/recommendations', { params: { topN: 15, includeDone: includeDone.value } }).then(res => list.value = res.data || [])
}

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
.header-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.setting-item{display:flex;align-items:center;justify-content:space-between;gap:12px;width:220px}
.reason-wrap{display:flex;flex-direction:column;gap:8px;padding:6px 0}
.score-line,.tag-row{display:flex;gap:6px;flex-wrap:wrap}
.reason-text{color:#475569;line-height:1.6}
</style>
