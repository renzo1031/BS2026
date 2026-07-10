<template>
  <section class="page">
    <h1 class="page-title">处理概览</h1>
    <div v-if="loading" class="state-block"><n-spin size="large" /></div>
    <n-result v-else-if="errorMessage" status="error" title="统计信息加载失败" :description="errorMessage">
      <template #footer><n-button @click="load">重试</n-button></template>
    </n-result>
    <div v-else class="grid">
      <n-card title="总申请量"><strong class="num">{{ stats.total }}</strong></n-card>
      <n-card title="待受理"><strong class="num">{{ stats.submitted }}</strong></n-card>
      <n-card title="处理中"><strong class="num">{{ stats.processing }}</strong></n-card>
      <n-card title="已完成"><strong class="num">{{ stats.finished }}</strong></n-card>
      <n-card title="已驳回"><strong class="num">{{ stats.rejected }}</strong></n-card>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { NButton, NCard, NResult, NSpin } from 'naive-ui'
import http from '../api/http'

const stats = ref({ total: 0, submitted: 0, processing: 0, finished: 0, rejected: 0 })
const loading = ref(false)
const errorMessage = ref('')

onMounted(load)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    stats.value = await http.get('/admin/stats')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.num {
  font-size: 34px;
  color: #0f766e;
}
</style>
