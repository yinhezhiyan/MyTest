<template>
  <div class="card">
    <h3>学习看板</h3>
    <div v-if="overview">答题总数: {{overview.totalAnswers}}，正确率: {{overview.accuracy}}%，历史推荐: {{overview.recommendedCount}}</div>
    <div style="margin-top:10px">今日推荐预览：</div>
    <ul><li v-for="i in recs" :key="i.exercise.id">{{ i.exercise.title }} - {{ i.reason }}</li></ul>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import request from '@/utils/request'
const overview = ref(null)
const recs = ref([])
request.get('/api/statistics/learning-overview').then(res => overview.value = res.data)
request.get('/api/student/recommendations', { params: { limit: 5 } }).then(res => recs.value = res.data || [])
</script>
