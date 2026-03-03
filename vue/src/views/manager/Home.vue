<template>
  <div>
    <div v-if="user.role === 'ADMIN'" class="card">
      <div class="header-row">
        <h2>本学科题库管理</h2>
        <el-button type="primary" @click="openAdd">插入题目</el-button>
      </div>
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
            <el-table-column label="操作" width="100">
              <template #default="scope"><el-button link type="danger" @click="del(scope.row.id)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div v-else class="card">
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

    <el-dialog v-model="addVisible" title="插入题目" width="620px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="章节"><el-input v-model="form.chapter"/></el-form-item>
        <el-form-item label="题干"><el-input type="textarea" v-model="form.stem"/></el-form-item>
        <el-form-item label="选项A"><el-input v-model="form.optionA"/></el-form-item>
        <el-form-item label="选项B"><el-input v-model="form.optionB"/></el-form-item>
        <el-form-item label="选项C"><el-input v-model="form.optionC"/></el-form-item>
        <el-form-item label="选项D"><el-input v-model="form.optionD"/></el-form-item>
        <el-form-item label="答案"><el-select v-model="form.answer" style="width:120px"><el-option value="A"/><el-option value="B"/><el-option value="C"/><el-option value="D"/></el-select></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis"/></el-form-item>
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
  </div>
</template>

<script setup>
import {computed, reactive, ref} from 'vue'
import request from '@/utils/request'
import {ElMessage, ElMessageBox} from 'element-plus'

const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const displayName = computed(() => user.name || user.username || '用户')
const chapters = ref([])
const summary = reactive({ todayTotal: 0, todayCorrect: 0, todayWrong: 0 })
const addVisible = ref(false)
const form = reactive({ chapter:'', stem:'', optionA:'', optionB:'', optionC:'', optionD:'', answer:'A', analysis:'', attachmentUrl:'' })
const uploadUrl = import.meta.env.VITE_BASE_URL + '/files/upload'

const loadAdmin = ()=> request.get('/admin/question-bank/chapters').then(res=> chapters.value = res.data || [])
const loadStudent = ()=> request.get('/api/profile/summary').then(res=> Object.assign(summary, res.data || {}))

const openAdd = ()=> { Object.assign(form, { chapter:'', stem:'', optionA:'', optionB:'', optionC:'', optionD:'', answer:'A', analysis:'', attachmentUrl:'' }); addVisible.value = true }
const onUpload = (res)=> { form.attachmentUrl = res.data }
const save = ()=> request.post('/admin/question-bank/exercise', form).then(res=> { if(res.code==='200'){ ElMessage.success('新增成功'); addVisible.value=false; loadAdmin() } else ElMessage.error(res.msg) })
const del = (id)=> ElMessageBox.confirm('确定删除该题吗？','提示',{type:'warning'}).then(()=> request.delete('/admin/question-bank/exercise/'+id).then(res=> { if(res.code==='200'){ ElMessage.success('删除成功'); loadAdmin()} else ElMessage.error(res.msg)}))

if (user.role === 'ADMIN') loadAdmin(); else loadStudent()
</script>

<style scoped>
.card{background:#fff;padding:16px;border-radius:12px}
.header-row{display:flex;justify-content:space-between;align-items:center;margin-bottom:12px}
.profile{display:flex;gap:16px;align-items:center}
.stats{display:flex;gap:40px}
</style>
