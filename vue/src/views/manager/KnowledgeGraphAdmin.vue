<template>
  <div class="grid-wrap">
    <div class="card full-width">
      <div class="header-row">
        <div>
          <h2>知识图谱管理</h2>
          <div class="subline">维护本学科知识点之间的先修、包含、关联关系，并查看全局图谱。</div>
        </div>
        <div class="action-row">
          <el-button @click="load">刷新</el-button>
          <el-button type="primary" @click="openCreate">新增关系</el-button>
        </div>
      </div>
      <KnowledgeGraphPanel :graph-data="graphData" title="当前学科知识图谱" />
    </div>

    <div class="card full-width">
      <div class="header-row">
        <h3>知识点清单</h3>
        <el-tag>{{ points.length }} 个知识点</el-tag>
      </div>
      <el-table :data="points" size="small" max-height="320">
        <el-table-column prop="kp_name" label="知识点" min-width="180"/>
        <el-table-column prop="exercise_count" label="关联题目数" width="110"/>
        <el-table-column prop="source_type" label="来源" width="100"/>
        <el-table-column label="章节" min-width="220">
          <template #default="scope">{{ (scope.row.chapter_refs || []).join('、') || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card full-width">
      <div class="header-row">
        <h3>知识关系维护</h3>
        <el-tag type="success">支持批量维护</el-tag>
      </div>
      <el-table :data="relations" size="small">
        <el-table-column prop="source_kp" label="源知识点" min-width="180"/>
        <el-table-column prop="target_kp" label="目标知识点" min-width="180"/>
        <el-table-column prop="relation_type" label="关系类型" width="120"/>
        <el-table-column prop="weight" label="权重" width="90"/>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识关系' : '新增知识关系'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="源知识点">
          <el-select v-model="form.sourceKp" filterable allow-create default-first-option style="width:100%">
            <el-option v-for="item in pointOptions" :key="`source-${item}`" :label="item" :value="item"/>
          </el-select>
        </el-form-item>
        <el-form-item label="目标知识点">
          <el-select v-model="form.targetKp" filterable allow-create default-first-option style="width:100%">
            <el-option v-for="item in pointOptions" :key="`target-${item}`" :label="item" :value="item"/>
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型">
          <el-select v-model="form.relationType" style="width:100%">
            <el-option label="相关 related" value="related"/>
            <el-option label="先修 prerequisite" value="prerequisite"/>
            <el-option label="包含 contains" value="contains"/>
          </el-select>
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="0.1" :max="5" :step="0.1"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, reactive, ref} from 'vue'
import request from '@/utils/request'
import {ElMessage, ElMessageBox} from 'element-plus'
import KnowledgeGraphPanel from '@/components/KnowledgeGraphPanel.vue'

const relations = ref([])
const points = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, sourceKp: '', targetKp: '', relationType: 'related', weight: 1 })
const subject = computed(() => JSON.parse(localStorage.getItem('system-user') || '{}').subject || '')
const pointOptions = computed(() => points.value.map(item => item.kp_name))
const graphData = computed(() => ({
  summary: { subject: subject.value, nodeCount: points.value.length, edgeCount: relations.value.length, avgMastery: 0 },
  nodes: points.value.map(item => ({
    id: item.kp_name,
    label: item.kp_name,
    mastery: item.exercise_count ? 0.6 : 0.15,
    weakness: 0,
    totalTimes: item.exercise_count || 0,
    wrongTimes: 0,
    chapters: item.chapter_refs || [],
    status: item.exercise_count ? 'LEARNING' : 'UNSEEN'
  })),
  edges: relations.value.map(item => ({
    id: item.id,
    source: item.source_kp,
    target: item.target_kp,
    relationType: item.relation_type,
    weight: item.weight
  })),
  weakTop: [],
  masteryTop: []
}))

const load = () => Promise.all([
  request.get('/admin/knowledge-graph/relations').then(res => relations.value = res.data || []),
  request.get('/admin/knowledge-graph/points').then(res => points.value = res.data || [])
])

const resetForm = () => {
  Object.assign(form, { id: null, sourceKp: '', targetKp: '', relationType: 'related', weight: 1 })
}
const openCreate = () => { resetForm(); dialogVisible.value = true }
const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    sourceKp: row.source_kp,
    targetKp: row.target_kp,
    relationType: row.relation_type,
    weight: Number(row.weight || 1)
  })
  dialogVisible.value = true
}
const save = () => {
  const payload = { sourceKp: form.sourceKp, targetKp: form.targetKp, relationType: form.relationType, weight: form.weight }
  const req = form.id
    ? request.put(`/admin/knowledge-graph/relations/${form.id}`, payload)
    : request.post('/admin/knowledge-graph/relations', payload)
  req.then(res => {
    if (res.code === '200') {
      ElMessage.success(form.id ? '关系已更新' : '关系已创建')
      dialogVisible.value = false
      load()
    } else ElMessage.error(res.msg)
  })
}
const remove = (id) => ElMessageBox.confirm('确认删除该知识关系吗？', '提示', { type: 'warning' }).then(() => {
  request.delete(`/admin/knowledge-graph/relations/${id}`).then(res => {
    if (res.code === '200') {
      ElMessage.success('删除成功')
      load()
    } else ElMessage.error(res.msg)
  })
})

load()
</script>

<style scoped>
.grid-wrap{display:grid;gap:16px}
.full-width{width:100%}
.card{background:#fff;padding:16px;border-radius:12px}
.header-row{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:12px}
.subline{margin-top:6px;color:#64748b;font-size:13px}
.action-row{display:flex;gap:8px;flex-wrap:wrap}
</style>
