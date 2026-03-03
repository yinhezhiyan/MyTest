<template>
  <div class="card" v-if="q.id">
    <h3>{{ q.chapter }} · {{ q.id }}</h3>
    <p>{{ q.stem }}</p>
    <el-radio-group v-model="chosen" @change="submit">
      <el-radio v-for="k in ['A','B','C','D']" :key="k" :label="k" :class="wrong===k ? 'wrong' : ''">{{k}}. {{ q['option'+k] }}</el-radio>
    </el-radio-group>
    <div style="margin-top:12px"><el-button @click="nextRandom">下一题</el-button></div>
  </div>
</template>
<script setup>
import {onMounted, reactive, ref} from 'vue';
import request from '@/utils/request';
import {ElMessage} from 'element-plus';
import {useRoute} from 'vue-router';
const route = useRoute();
const q = reactive({});
const chosen = ref('');
const wrong = ref('');
const loadById = (id)=> request.get('/api/exercises/'+id).then(res=>Object.assign(q,res.data||{}))
const nextRandom = ()=>{chosen.value='';wrong.value='';request.get('/api/exercises/random').then(res=>Object.assign(q,res.data||{}))}
const submit = ()=>{
  request.post('/api/answers/submit',{exerciseId:q.id, chosenOption:chosen.value}).then(res=>{
    if (res.data.isCorrect){ElMessage.success('回答正确，自动下一题');nextRandom()} else {wrong.value=chosen.value;ElMessage.error('错误，请继续作答')}
  })
}
onMounted(()=> route.query.id ? loadById(route.query.id) : nextRandom())
</script>
<style scoped>.card{background:#fff;padding:18px;border-radius:12px}.wrong{color:#f56c6c}</style>
