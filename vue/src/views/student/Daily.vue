<template>
  <div class="card">
    <h2>今日个性拓展</h2>
    <el-empty v-if="!list.length" description="暂无推荐"/>
    <el-timeline v-else>
      <el-timeline-item v-for="item in list" :key="item.exerciseId">
        <div class="item-title">{{ item.stem }}</div>
        <el-button link type="primary" @click="go(item.exerciseId)">开始练习</el-button>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>
<script setup>
import {onMounted, ref} from 'vue';
import request from '@/utils/request';
import router from '@/router';
const user = JSON.parse(localStorage.getItem('system-user') || '{}')
const list = ref([])
const load = ()=> request.get('/api/daily').then(res=> list.value = res.data || [])
const go = (id)=> router.push(`/manager/${user.subject}/practice?id=${id}`)
onMounted(load)
</script>
<style scoped>.card{background:#fff;padding:18px;border-radius:12px}.item-title{font-weight:600}</style>
