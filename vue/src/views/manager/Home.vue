<template>
  <div>
    <div v-if="user.role === 'ADMIN'" class="card" style="margin-bottom: 16px;">
      <div class="header-row">
        <h2>本学科题库管理</h2>
        <div class="action-row">
          <el-button @click="loadImportHelp">刷新推荐题库说明</el-button>
          <el-button @click="importVisible = true">批量导入题库</el-button>
          <el-button type="success" @click="goGraphAdmin">知识图谱管理</el-button>
          <el-button type="primary" @click="openAdd">插入题目</el-button>
        </div>
      </div>
      <el-alert type="info" show-icon :closable="false" style="margin-bottom: 12px;"
                title="支持批量导入 JSON 题库，并同步刷新知识点与知识关系图谱。"/>
      <el-collapse>
        <el-collapse-item v-for="item in chapters" :key="item.chapter" :name="item.chapter">
          <template #title>{{ item.chapter }}（{{ item.total }}）</template>
          <el-table :data="item.exercises" size="small">
            <el-table-column prop="id" label="题号" width="180"/>
            <el-table-column prop="stem" label="题目"/>
            <el-table-column label="附件" width="120">
              <template #default="scope">
                <el-link v-if="scope.row.attachment_url" :href="scope.row.attachment_url" target="_blank">查看附件</el-link>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="scope">
                <el-button link type="primary" @click="openEdit(scope.row.id)">编辑</el-button>
                <el-button link type="danger" @click="del(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div v-else class="student-grid">
      <div class="card">
        <h2>个人信息</h2>
        <div class="profile">
          <el-avatar :src="user.avatar" :size="64">{{ displayName.slice(0,1) }}</el-avatar>
          <div>
            <div>昵称：{{ displayName }}</div>
            <div>账号：{{ user.username }}</div>
          </div>
        </div>
        <h3 style="margin-top:16px">今日答题情况</h3>
        <div class="stats">
          <el-statistic title="今日作答" :value="summary.todayTotal || 0"/>
          <el-statistic title="答对" :value="summary.todayCorrect || 0"/>
          <el-statistic title="答错" :value="summary.todayWrong || 0"/>
        </div>
      </div>

      <div class="card full-width">
        <KnowledgeGraphPanel :graph-data="knowledgeGraph" title="我的知识图谱" />
      </div>
    </div>

    <el-dialog v-model="addVisible" :title="form.id ? '编辑题目' : '插入题目'" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="章节"><el-input v-model="form.chapter"/></el-form-item>
        <el-form-item label="题干"><el-input type="textarea" v-model="form.stem" :rows="4"/></el-form-item>
        <el-form-item label="选项A"><el-input v-model="form.optionA"/></el-form-item>
        <el-form-item label="选项B"><el-input v-model="form.optionB"/></el-form-item>
        <el-form-item label="选项C"><el-input v-model="form.optionC"/></el-form-item>
        <el-form-item label="选项D"><el-input v-model="form.optionD"/></el-form-item>
        <el-form-item label="答案"><el-select v-model="form.answer" style="width:120px"><el-option value="A"/><el-option value="B"/><el-option value="C"/><el-option value="D"/></el-select></el-form-item>
        <el-form-item label="难度"><el-input-number v-model="form.difficulty" :min="1" :max="5"/></el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="form.knowledgePointText" placeholder="多个知识点用中文逗号或英文逗号分隔"/>
        </el-form-item>
        <el-form-item label="解析"><el-input type="textarea" v-model="form.analysis" :rows="3"/></el-form-item>
        <el-form-item label="附件">
          <el-upload :action="uploadUrl" :on-success="onUpload" :limit="1">
            <el-button>上传图片/Word</el-button>
          </el-upload>
          <el-link v-if="form.attachmentUrl" :href="form.attachmentUrl" target="_blank" style="margin-left:8px">已上传附件</el-link>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="批量导入题库" width="560px">
      <el-form :model="importForm" label-width="100px">
        <el-form-item label="目标学科">
          <el-input :model-value="subjectText" disabled/>
        </el-form-item>
        <el-form-item label="JSON 文件路径">
          <el-input v-model="importForm.filePath" placeholder="例如 data/question-bank/ds.json"/>
        </el-form-item>
        <el-form-item>
          <el-alert :closable="false" type="warning" show-icon title="导入会按当前学科覆盖 MAIN 题库，并自动重建知识图谱节点。" />
        </el-form-item>
        <el-form-item v-if="importResult.processed">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处理数量">{{ importResult.processed }}</el-descriptions-item>
            <el-descriptions-item label="题库总量">{{ importResult.total }}</el-descriptions-item>
            <el-descriptions-item label="新增">{{ importResult.inserted }}</el-descriptions-item>
            <el-descriptions-item label="更新">{{ importResult.updated }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" @click="importBank">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, reactive, ref} from 'vue'
import request from '@/utils/request'
import {ElMessage, ElMessageBox} from 'element-plus'
import router from '@/router'
import KnowledgeGraphPanel from '@/components/KnowledgeGraphPanel.vue'

const user = reactive(JSON.parse(localStorage.getItem('system-user') || '{}'))
const displayName = computed(() => user.name || user.username || '用户')
const subjectText = computed(() => ({DS: '数据结构', OS: '操作系统', CN: '计网', CO: '计组'})[user.subject] || user.subject)
const chapters = ref([])
const summary = reactive({ todayTotal: 0, todayCorrect: 0, todayWrong: 0 })
const knowledgeGraph = ref({})
const addVisible = ref(false)
const importVisible = ref(false)
const form = reactive({ id:'', chapter:'', stem:'', optionA:'', optionB:'', optionC:'', optionD:'', answer:'A', analysis:'', attachmentUrl:'', difficulty: 2, knowledgePointText:'' })
const importForm = reactive({ filePath: '' })
const importResult = reactive({})
const uploadUrl = import.meta.env.VITE_API_BASE_URL + '/files/upload'

const syncUserFromStorage = () => {
  const latest = JSON.parse(localStorage.getItem('system-user') || '{}')
  Object.keys(user).forEach(k => delete user[k])
  Object.assign(user, latest)
}

const normalizeKnowledgePoints = (text) => JSON.stringify(
  String(text || '')
    .split(/[，,]/)
    .map(item => item.trim())
    .filter(Boolean)
)

const fillForm = (payload = {}) => {
  Object.assign(form, {
    id: payload.id || '',
    chapter: payload.chapter || '',
    stem: payload.stem || '',
    optionA: payload.optionA || '',
    optionB: payload.optionB || '',
    optionC: payload.optionC || '',
    optionD: payload.optionD || '',
    answer: payload.answer || 'A',
    analysis: payload.analysis || '',
    attachmentUrl: payload.attachmentUrl || '',
    difficulty: payload.difficulty || 2,
    knowledgePointText: Array.isArray(payload.knowledgePoints)
      ? payload.knowledgePoints.join('，')
      : (() => {
        try {
          return JSON.parse(payload.knowledgePoints || '[]').join('，')
        } catch (e) {
          return ''
        }
      })()
  })
}

const onUserProfileUpdated = (event) => {
  if (event?.detail && Object.keys(event.detail).length > 0) {
    Object.keys(user).forEach(k => delete user[k])
    Object.assign(user, event.detail)
    return
  }
  syncUserFromStorage()
}

const loadAdmin = ()=> request.get('/admin/question-bank/chapters').then(res=> chapters.value = res.data || [])
const loadStudent = ()=> Promise.all([
  request.get('/api/profile/summary').then(res=> Object.assign(summary, res.data || {})),
  request.get('/api/knowledge-graph/me').then(res => knowledgeGraph.value = res.data || {})
])

const openAdd = ()=> { fillForm(); addVisible.value = true }
const openEdit = (id) => {
  request.get(`/admin/question-bank/exercise/${id}`).then(res => {
    fillForm(res.data || {})
    addVisible.value = true
  })
}
const onUpload = (res)=> { form.attachmentUrl = res.data }
const save = ()=> {
  const payload = {
    ...form,
    knowledgePoints: normalizeKnowledgePoints(form.knowledgePointText)
  }
  const req = form.id
    ? request.put(`/admin/question-bank/exercise/${form.id}`, payload)
    : request.post('/admin/question-bank/exercise', payload)
  req.then(res=> {
    if(res.code==='200'){
      ElMessage.success(form.id ? '编辑成功' : '新增成功')
      addVisible.value=false
      loadAdmin()
    } else ElMessage.error(res.msg)
  })
}
const del = (id)=> ElMessageBox.confirm('确定删除该题吗？','提示',{type:'warning'}).then(()=> request.delete('/admin/question-bank/exercise/'+id).then(res=> { if(res.code==='200'){ ElMessage.success('删除成功'); loadAdmin()} else ElMessage.error(res.msg)}))
const importBank = () => {
  request.post('/admin/question-bank/import', { subject: user.subject, filePath: importForm.filePath }).then(res => {
    if (res.code === '200') {
      Object.assign(importResult, res.data || {})
      ElMessage.success('题库导入成功')
      loadAdmin()
    } else ElMessage.error(res.msg)
  })
}
const loadImportHelp = () => {
  importForm.filePath = `data/question-bank/${String(user.subject || '').toLowerCase()}.json`
}
const goGraphAdmin = () => router.push(`/manager/${user.subject}/knowledge-graph`)

onMounted(() => {
  window.addEventListener('user-profile-updated', onUserProfileUpdated)
  if (user.role === 'ADMIN') {
    loadImportHelp()
    loadAdmin()
  } else {
    loadStudent()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('user-profile-updated', onUserProfileUpdated)
})
</script>

<style scoped>
.card{background:#fff;padding:16px;border-radius:12px}
.header-row{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;gap:12px;flex-wrap:wrap}
.action-row{display:flex;gap:8px;flex-wrap:wrap}
.profile{display:flex;gap:16px;align-items:center}
.stats{display:flex;gap:40px;flex-wrap:wrap}
.student-grid{display:grid;gap:16px}
.full-width{width:100%}
</style>
