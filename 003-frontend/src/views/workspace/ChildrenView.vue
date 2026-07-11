<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ChildView, PageResult } from '@/types/models'
import type { ChildFormPayload } from '@/types/forms'
import { riskLevelLabel, statusLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import ChildFormModal from '@/components/child/ChildFormModal.vue'

const auth = useAuthStore()
const loading = shallowRef(false)
const saving = shallowRef(false)
const formOpen = shallowRef(false)
const editing = shallowRef<ChildView | null>(null)
const data = shallowRef<PageResult<ChildView>>({ items: [], total: 0, page: 1, size: 10 })
const filters = reactive({ keyword: '', status: undefined as string | undefined })
const canWrite = computed(() => auth.hasPermission('child:write'))

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<ChildView>>('/children', {
      page, size: data.value.size, keyword: filters.keyword || undefined, status: filters.status,
    })
  } finally {
    loading.value = false
  }
}

function create() {
  editing.value = null
  formOpen.value = true
}

function edit(record: ChildView) {
  editing.value = record
  formOpen.value = true
}

async function save(payload: ChildFormPayload) {
  saving.value = true
  try {
    if (editing.value) await api.put<ChildView>(`/children/${editing.value.id}`, payload)
    else await api.post<ChildView>('/children', payload)
    message.success(editing.value ? '档案已更新' : '档案已创建')
    formOpen.value = false
    await load(data.value.page)
  } finally {
    saving.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="page-stack">
    <PageHeader title="儿童档案" subtitle="敏感字段已加密存储，列表仅对授权人员开放。">
      <template #actions>
        <a-button v-if="canWrite" type="primary" @click="create"><PlusOutlined /> 新建档案</a-button>
      </template>
    </PageHeader>
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-input v-model:value="filters.keyword" allow-clear placeholder="档案编号或服务区域" @press-enter="load(1)" />
          <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 160px" :options="['DRAFT','PENDING_REVIEW','ACTIVE','REJECTED','ARCHIVED'].map(value => ({ value, label: statusLabel(value) }))" />
          <a-button type="primary" @click="load(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="data.items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 980 }">
        <a-table-column title="档案编号" data-index="fileNo" :width="190" />
        <a-table-column title="姓名" data-index="name" :width="100" />
        <a-table-column title="服务区域" data-index="region" :width="150" />
        <a-table-column title="学段" data-index="schoolStage" :width="100" />
        <a-table-column title="风险" :width="90"><template #default="{ record }">{{ riskLevelLabel(record.riskLevel) }}</template></a-table-column>
        <a-table-column title="负责人" data-index="creatorName" :width="130" />
        <a-table-column title="状态" :width="120"><template #default="{ record }"><StatusTag :status="record.status" /></template></a-table-column>
        <a-table-column title="更新时间" data-index="updatedAt" :width="180" />
        <a-table-column title="操作" :width="150" fixed="right">
          <template #default="{ record }">
            <a-space>
              <RouterLink :to="`/app/children/${record.id}`">详情</RouterLink>
              <a-button v-if="canWrite && ['DRAFT','REJECTED'].includes(record.status)" type="link" @click="edit(record)">编辑</a-button>
            </a-space>
          </template>
        </a-table-column>
        <template #emptyText><a-empty description="暂无儿童档案" /></template>
      </a-table>
      <a-pagination v-if="data.total > data.size" class="pagination" :current="data.page" :page-size="data.size" :total="data.total" :show-size-changer="false" @change="load" />
    </div>
    <ChildFormModal v-model:open="formOpen" :record="editing" :loading="saving" @submit="save" />
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}
</style>
