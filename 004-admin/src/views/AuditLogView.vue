<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { AuditView, PageResult } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import AuditLogTable from '@/components/governance/AuditLogTable.vue'

const actionName = shallowRef('')
const pager = usePagedRequest<AuditView>((page, size) =>
  request<PageResult<AuditView>>({ url: '/admin/audit-logs', params: { page, size, actionName: actionName.value || undefined } }),
  20,
)

function search() {
  pager.changePage(1)
}

onMounted(pager.load)
</script>

<template>
  <div class="page">
    <PageHeader title="审计日志" description="关键审核、处置与申诉操作不可静默覆盖，可通过 requestId 追踪请求。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新</ElButton></template>
    </PageHeader>
    <section class="page-section workbench-section">
      <form class="filters workbench-toolbar" @submit.prevent="search">
        <ElInput v-model="actionName" clearable placeholder="按操作名精确筛选，例如 ACTION_REPORT" aria-label="操作名" @clear="search" />
        <ElButton native-type="submit" type="primary" :icon="Search">查询</ElButton>
      </form>
      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="暂无审计日志" @retry="pager.load">
        <AuditLogTable :rows="pager.records.value" />
        <ElPagination class="pagination" :current-page="pager.page.value" :page-size="pager.size.value" :total="pager.total.value" layout="total, prev, pager, next" @current-change="pager.changePage" />
      </AsyncState>
    </section>
  </div>
</template>

<style scoped>
.filters { display: flex; max-width: 540px; gap: 10px; }
.pagination { justify-content: flex-end; margin-top: 18px; }
</style>
