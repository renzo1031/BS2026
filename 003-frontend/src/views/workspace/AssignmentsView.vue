<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { api } from '@/api/http'
import type { AssignmentView, PageResult } from '@/types/models'
import { statusLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const loading = shallowRef(false)
const data = shallowRef<PageResult<AssignmentView>>({ items: [], total: 0, page: 1, size: 10 })
const filters = reactive({ status: undefined as string | undefined })

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<AssignmentView>>('/assignments', {
      page, size: data.value.size, status: filters.status,
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="page-stack">
    <PageHeader title="服务任务" subtitle="查看任务状态、服务对象和回访记录。" />
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-select
            v-model:value="filters.status"
            allow-clear
            placeholder="全部状态"
            style="width: 180px"
            :options="['ASSIGNED','IN_PROGRESS','PENDING_ACCEPTANCE','COMPLETED','TERMINATED'].map(value => ({ value, label: statusLabel(value) }))"
          />
          <a-button type="primary" @click="load(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="data.items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 900 }">
        <a-table-column title="任务编号" data-index="id" :width="110" />
        <a-table-column title="需求编号" data-index="requestNo" :width="170" />
        <a-table-column title="需求标题" data-index="requestTitle" :width="220" />
        <a-table-column title="服务区域" data-index="region" :width="150" />
        <a-table-column title="志愿者" data-index="volunteerName" :width="130" />
        <a-table-column title="状态" :width="120">
          <template #default="{ record }"><StatusTag :status="record.status" /></template>
        </a-table-column>
        <a-table-column title="创建时间" data-index="createdAt" :width="180" />
        <a-table-column title="操作" :width="100" fixed="right">
          <template #default="{ record }"><RouterLink :to="`/app/assignments/${record.id}`">详情</RouterLink></template>
        </a-table-column>
        <template #emptyText><a-empty description="暂无服务任务" /></template>
      </a-table>
      <a-pagination
        v-if="data.total > data.size"
        class="pagination"
        :current="data.page"
        :page-size="data.size"
        :total="data.total"
        :show-size-changer="false"
        @change="load"
      />
    </div>
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}
</style>
