<template>
  <div class="grid-wrap">
    <div class="card full-width">
      <KnowledgeGraphPanel :graph-data="overview" title="本学科知识图谱" />
    </div>

    <div class="card full-width">
      <div class="header-row">
        <div>
          <h2>知识图谱管理</h2>
          <div class="subline">恢复统计视图，并按当前学科全部学生的答题数据聚合展示。</div>
        </div>
        <div class="action-row">
          <el-button @click="load">刷新</el-button>
          <el-button @click="batchVisible = true">批量维护关系</el-button>
          <el-button type="primary" @click="openCreate">新增关系</el-button>
        </div>
      </div>

      <div class="summary-strip">
        <div class="summary-item"><span>知识点总数</span><strong>{{ overview.summary?.nodeCount || 0 }}</strong></div>
        <div class="summary-item"><span>关系边总数</span><strong>{{ overview.summary?.edgeCount || 0 }}</strong></div>
        <div class="summary-item"><span>激活知识点</span><strong>{{ overview.summary?.activatedNodeCount || 0 }}</strong></div>
        <div class="summary-item"><span>薄弱知识点</span><strong>{{ overview.summary?.weakNodeCount || 0 }}</strong></div>
        <div class="summary-item"><span>已掌握知识点</span><strong>{{ overview.summary?.masteredNodeCount || 0 }}</strong></div>
      </div>

      <el-table :data="points" size="small" max-height="420">
        <el-table-column prop="kp_name" label="知识点" min-width="180"/>
        <el-table-column prop="exercise_count" label="关联题目数" width="120"/>
        <el-table-column label="当前权重" width="180">
          <template #default="scope">
            <div class="weight-cell">
              <el-input-number v-model="scope.row.weight" :min="0.1" :max="5" :step="0.1"/>
              <el-button link type="primary" @click="saveWeight(scope.row)">保存</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="source_type" label="来源" width="100"/>
        <el-table-column label="章节" min-width="260">
          <template #default="scope">{{ (scope.row.chapter_refs || []).join('、') || '-' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card full-width">
      <div class="header-row">
        <h3>知识关系维护</h3>
        <el-tag>{{ relations.length }} 条</el-tag>
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

    <el-dialog v-model="batchVisible" title="批量维护知识关系" width="700px">
      <el-alert type="info" show-icon :closable="false" style="margin-bottom: 12px;"
                title="每行一条：源知识点,目标知识点,关系类型,权重。关系类型可填 related / prerequisite / contains。"/>
      <el-input v-model="batchText" type="textarea" :rows="12" :placeholder="batchPlaceholder"/>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">提交批量维护</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, reactive, ref} from 'vue'
import request from '@/utils/request'
import {ElMessage, ElMessageBox} from 'element-plus'
import KnowledgeGraphPanel from '@/components/KnowledgeGraphPanel.vue'

const overview = ref({})
const relations = ref([])
const points = ref([])
const dialogVisible = ref(false)
const batchVisible = ref(false)
const batchText = ref('')
const batchPlaceholder = '例如：\n线性表,栈,prerequisite,1.2\n线性表,队列,prerequisite,1.2'
const form = reactive({ id: null, sourceKp: '', targetKp: '', relationType: 'related', weight: 1 })
const pointOptions = computed(() => points.value.map(item => item.kp_name))

const load = () => Promise.all([
  request.get('/admin/knowledge-graph/overview').then(res => overview.value = res.data || {}),
  request.get('/admin/knowledge-graph/relations').then(res => relations.value = res.data || []),
  request.get('/admin/knowledge-graph/points').then(res => points.value = (res.data || []).map(item => ({ ...item, weight: Number(item.weight || 1) })))
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
const saveWeight = (row) => {
  request.put(`/admin/knowledge-graph/points/${row.id}/weight`, { weight: row.weight }).then(res => {
    if (res.code === '200') {
      ElMessage.success(`已更新 ${row.kp_name} 的权重`)
      load()
    } else ElMessage.error(res.msg)
  })
}
const saveBatch = () => {
  const payload = batchText.value
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .map(line => {
      const [sourceKp, targetKp, relationType = 'related', weight = '1'] = line.split(',').map(item => item.trim())
      return { sourceKp, targetKp, relationType, weight: Number(weight || 1) }
    })
  if (!payload.length) {
    ElMessage.warning('请先输入批量关系内容')
    return
  }
  request.post('/admin/knowledge-graph/relations/batch', payload).then(res => {
    if (res.code === '200') {
      ElMessage.success('批量维护完成')
      batchVisible.value = false
      batchText.value = ''
      load()
    } else ElMessage.error(res.msg)
  })
}

load()
</script>

<style scoped>
.grid-wrap{display:grid;gap:16px}
.full-width{width:100%}
.card{background:#fff;padding:16px;border-radius:12px}
.header-row{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;margin-bottom:12px}
.subline{margin-top:6px;color:#64748b;font-size:13px}
.action-row,.weight-cell{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
.summary-strip{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin-bottom:16px}
.summary-item{padding:12px 14px;border-radius:12px;background:#f8fafc;border:1px solid #e5e7eb;display:flex;flex-direction:column;gap:4px}
.summary-item span{font-size:12px;color:#64748b}
.summary-item strong{font-size:22px;color:#111827}
</style>
