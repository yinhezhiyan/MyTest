<template>
  <div class="card">
    <h2>习题推荐</h2>
    <el-table :data="list">
      <el-table-column prop="chapter" label="章节" width="140"/>
      <el-table-column prop="stem" label="题目"/>
      <el-table-column prop="reason" label="推荐原因"/>
      <el-table-column label="操作" width="120">
        <template #default="scope"><el-button type="primary" link @click="go(scope.row.exerciseId)">去作答</el-button></template>
      </el-table-column>
    </el-table>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue';
import request from '@/utils/request';
import router from '@/router';
const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const list = ref([])
onMounted(()=> request.get('/api/recommendations').then(res=> list.value=res.data||[]))
const go = (id)=> router.push(`/manager/${user.subject}/practice?id=${id}`)
</script>
<style scoped>.card{background:#fff;padding:18px;border-radius:12px}</style>
