<template>
  <div class="card">
    <h2>答题记录</h2>
    <div style="margin-bottom:10px;display:flex;gap:8px">
      <el-input v-model="chapter" placeholder="章节筛选" style="width:200px"/>
      <el-select v-model="correct" clearable placeholder="正确与否" style="width:140px">
        <el-option :value="1" label="正确"/><el-option :value="0" label="错误"/>
      </el-select>
      <el-button @click="load" type="primary">筛选</el-button>
    </div>
    <el-table :data="list">
      <el-table-column prop="chapter" label="章节" width="120"/>
      <el-table-column prop="stem" label="题目"/>
      <el-table-column prop="answered_at" label="时间" width="180"/>
      <el-table-column prop="is_correct" label="是否正确" width="90">
        <template #default="scope">{{ scope.row.is_correct ? '是':'否' }}</template>
      </el-table-column>
      <el-table-column prop="chosen_option" label="选择" width="70"/>
      <el-table-column prop="correct_answer" label="正确答案" width="90"/>
    </el-table>
  </div>
</template>
<script setup>
import {ref} from 'vue';
import request from '@/utils/request';
const list = ref([]); const chapter = ref(''); const correct = ref(null)
const load = ()=> request.get('/api/answers/records',{params:{chapter:chapter.value||undefined, correct:correct.value}}).then(res=> list.value=res.data||[])
load()
</script>
<style scoped>.card{background:#fff;padding:18px;border-radius:12px}</style>
