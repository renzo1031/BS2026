<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { api } from '@/api/http'
import type { AuditView, PageResult } from '@/types/models'
import { auditActionLabel, businessTypeLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const loading = shallowRef(false)
const businessType = shallowRef<string | undefined>()
const data = shallowRef<PageResult<AuditView>>({ items: [], total: 0, page: 1, size: 20 })

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<AuditView>>('/admin/audit-logs', {
      page, size: data.value.size, businessType: businessType.value,
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="page-stack">
    <PageHeader title="审计日志" subtitle="记录账号、审核、匹配和关键状态变化，不保存明文敏感信息。" />
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-select v-model:value="businessType" allow-clear placeholder="全部业务类型" style="width: 190px" :options="['USER','AUTH_SESSION','CHILD','AID_REQUEST','AID_APPLICATION','ASSIGNMENT','VOLUNTEER','ROLE','DEPARTMENT'].map(value => ({ value, label: businessTypeLabel(value) }))" />
          <a-button type="primary" @click="load(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="data.items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1180 }">
        <a-table-column title="时间" data-index="createdAt" :width="180" />
        <a-table-column title="操作人" data-index="username" :width="130"><template #default="{ text }">{{ text || '系统' }}</template></a-table-column>
        <a-table-column title="动作" :width="190"><template #default="{ record }">{{ auditActionLabel(record.action) }}</template></a-table-column>
        <a-table-column title="业务类型" :width="150"><template #default="{ record }">{{ businessTypeLabel(record.businessType) }}</template></a-table-column>
        <a-table-column title="业务编号" data-index="businessId" :width="180" />
        <a-table-column title="原状态" :width="130"><template #default="{ record }"><StatusTag v-if="record.beforeStatus" :status="record.beforeStatus" /><span v-else>-</span></template></a-table-column>
        <a-table-column title="新状态" :width="130"><template #default="{ record }"><StatusTag v-if="record.afterStatus" :status="record.afterStatus" /><span v-else>-</span></template></a-table-column>
        <a-table-column title="说明" data-index="detail" :width="220" />
        <a-table-column title="来源 IP" data-index="ipAddress" :width="140" />
        <template #emptyText><a-empty description="暂无审计日志" /></template>
      </a-table>
      <a-pagination v-if="data.total > data.size" class="pagination" :current="data.page" :page-size="data.size" :total="data.total" :show-size-changer="false" @change="load" />
    </div>
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}
</style>
