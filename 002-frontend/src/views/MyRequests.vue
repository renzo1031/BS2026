<template>
  <section class="page">
    <div class="toolbar">
      <h1 class="page-title">我的申请</h1>
      <n-space class="list-actions">
        <n-select
          v-model:value="status"
          :options="requestStatusOptions"
          clearable
          placeholder="全部状态"
          style="width: 160px"
          @update:value="applyFilter"
        />
        <n-button type="primary" @click="router.push('/apply')">新申请</n-button>
      </n-space>
    </div>

    <n-result v-if="errorMessage" status="error" title="申请列表加载失败" :description="errorMessage">
      <template #footer><n-button @click="load">重试</n-button></template>
    </n-result>
    <template v-else>
      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="false"
        :scroll-x="900"
        :row-key="(row) => row.id"
      >
        <template #empty><n-empty description="暂无申请记录" /></template>
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
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NDataTable, NEmpty, NPagination, NResult, NSelect, NSpace, NTag } from 'naive-ui'
import http from '../api/http'
import { formatDateTime, requestStatusOptions, statusText, statusType } from '../utils/display'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const rows = ref([])
const page = ref(1)
const pageCount = ref(1)
const status = ref(null)

const columns = [
  { title: '申请编号', key: 'requestNo', width: 180 },
  { title: '标题', key: 'title', minWidth: 220, ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'status', width: 100,
    render: (row) => h(NTag, { type: statusType(row.status), bordered: false }, { default: () => statusText(row.status) })
  },
  { title: '提交时间', key: 'createdAt', width: 170, render: (row) => formatDateTime(row.createdAt) },
  {
    title: '操作', key: 'actions', width: 90, fixed: 'right',
    render: (row) => h(NButton, { size: 'small', onClick: () => router.push(`/requests/${row.id}`) }, { default: () => '详情' })
  }
]

onMounted(load)

function applyFilter() {
  page.value = 1
  load()
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await http.get('/requests/my', {
      params: { page: page.value, size: 10, status: status.value || undefined }
    })
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

<style scoped>
@media (max-width: 640px) {
  .list-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
