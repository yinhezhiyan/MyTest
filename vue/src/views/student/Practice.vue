<template>
  <ExerciseAnswerPanel :initial-id="initialId" :sequence-ids="sequenceIds" />
</template>

<script setup>
import {computed} from 'vue'
import {useRoute} from 'vue-router'
import ExerciseAnswerPanel from '@/components/ExerciseAnswerPanel.vue'

const route = useRoute()
const initialId = computed(() => String(route.query.id || ''))
const sequenceIds = computed(() => route.query.ids ? String(route.query.ids).split(',').filter(Boolean) : [])
</script>
