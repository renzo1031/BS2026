<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { api } from '@/api/http'
import type { ApplicationView } from '@/types/models'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const loading = shallowRef(false)
const items = shallowRef<ApplicationView[]>([])

async function load() {
  loading.value = true
  try {
    items.value = await api.get<ApplicationView[]>('/applications/mine')
  } finally {
    loading.value = false
  }
}

function withdraw(item: ApplicationView) {
  Modal.confirm({
    title: '确认撤回申请？',
    content: item.requestTitle,
    async onOk() {
      await api.post<void>(`/applications/${item.id}/withdraw`)
      message.success('申请已撤回')
      await load()
    },
  })
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <PageHeader title="我的申请" subtitle="查看申请状态，待处理申请可以主动撤回。" />
    <div class="surface">
      <a-table :data-source="items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 800 }">
        <a-table-column title="需求编号" data-index="requestNo" :width="170" />
        <a-table-column title="需求标题" data-index="requestTitle" :width="220" />
        <a-table-column title="申请说明" data-index="message" :ellipsis="true" />
        <a-table-column title="状态" :width="110">
          <template #default="{ record }"><StatusTag :status="record.status" /></template>
        </a-table-column>
        <a-table-column title="申请时间" data-index="createdAt" :width="180" />
        <a-table-column title="操作" :width="120" fixed="right">
          <template #default="{ record }">
            <a-button v-if="record.status === 'APPLIED'" type="link" danger @click="withdraw(record)">撤回</a-button>
            <RouterLink v-else-if="record.status === 'ACCEPTED'" to="/app/assignments">查看任务</RouterLink>
            <span v-else>-</span>
          </template>
        </a-table-column>
        <template #emptyText><a-empty description="暂无申请记录" /></template>
      </a-table>
    </div>
  </div>
</template>
