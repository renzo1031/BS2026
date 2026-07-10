<template>
  <section class="page">
    <h1 class="page-title">操作日志</h1>
    <n-result v-if="errorMessage" status="error" title="操作日志加载失败" :description="errorMessage">
      <template #footer><n-button @click="load">重试</n-button></template>
    </n-result>
    <template v-else>
      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="false"
        :scroll-x="980"
        :row-key="(row) => row.id"
      >
        <template #empty><n-empty description="暂无操作日志" /></template>
      </n-data-table>
      <n-pagination
        v-if="rows.length || pageCount > 1"
        v-model:page="page"
        :page-count="pageCount"
        class="pager"
        @update:page="load"
      />
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { NButton, NDataTable, NEmpty, NPagination, NResult } from 'naive-ui'
import http from '../api/http'
import { formatDateTime } from '../utils/display'

const rows = ref([])
const loading = ref(false)
const errorMessage = ref('')
const page = ref(1)
const pageCount = ref(1)
const columns = [
  { title: '操作人', key: 'operatorName', width: 130, render: (row) => row.operatorName || row.username || '-' },
  { title: '模块', key: 'module', width: 130 },
  { title: '动作', key: 'action', width: 150 },
  { title: '详情', key: 'detail', minWidth: 320, ellipsis: { tooltip: true } },
  { title: '时间', key: 'createdAt', width: 170, render: (row) => formatDateTime(row.createdAt) }
]

onMounted(load)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await http.get('/admin/logs', { params: { page: page.value, size: 10 } })
    rows.value = result?.records || []
    const size = result?.size || 10
    pageCount.value = Math.max(1, Math.ceil((result?.total || 0) / size))
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
