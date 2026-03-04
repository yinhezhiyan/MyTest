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
      <el-table-column label="操作" width="120">
        <template #default="scope"><el-button type="primary" link @click="go(scope.row.exerciseId)">去作答</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue';
import {MoreFilled} from '@element-plus/icons-vue'
import request from '@/utils/request';
import router from '@/router';
const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const includeDone = ref(localStorage.getItem('recommend-include-done') === '1')
const list = ref([])
const load = ()=> {
  localStorage.setItem('recommend-include-done', includeDone.value ? '1' : '0')
  request.get('/api/recommendations', { params: { includeDone: includeDone.value } }).then(res=> list.value=res.data||[])
}
onMounted(load)
const go = (id)=> {
  const ids = list.value.map(item => item.exerciseId)
  const index = ids.indexOf(id)
  router.push({
    path: `/manager/${user.subject}/practice`,
    query: {
      id,
      ids: ids.join(','),
      idx: index < 0 ? 0 : index
    }
  })
}
</script>
<style scoped>
.card{background:#fff;padding:18px;border-radius:12px}
.header-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:8px}
.setting-item{display:flex;align-items:center;justify-content:space-between;gap:12px;width:220px}
</style>
