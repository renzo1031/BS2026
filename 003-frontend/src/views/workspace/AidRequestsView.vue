<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { AidView, ChildView, PageResult } from '@/types/models'
import type { AidFormPayload } from '@/types/forms'
import { categoryLabel, priorityLabel, statusLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import AidFormModal from '@/components/aid/AidFormModal.vue'

const auth = useAuthStore()
const loading = shallowRef(false)
const saving = shallowRef(false)
const formOpen = shallowRef(false)
const editing = shallowRef<AidView | null>(null)
const children = shallowRef<ChildView[]>([])
const data = shallowRef<PageResult<AidView>>({ items: [], total: 0, page: 1, size: 10 })
const filters = reactive({ keyword: '', status: undefined as string | undefined, category: undefined as string | undefined })
const canWrite = computed(() => auth.hasPermission('aid:write'))
const categoryOptions = [
  { value: 'EDUCATION', label: '学习支持' }, { value: 'COMPANIONSHIP', label: '成长陪伴' },
  { value: 'LIFE_CARE', label: '生活关怀' }, { value: 'SAFETY', label: '安全关爱' },
  { value: 'PSYCHOLOGICAL', label: '心理支持' }, { value: 'OTHER', label: '其他支持' },
]

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<AidView>>('/aid-requests', {
      page, size: data.value.size, keyword: filters.keyword || undefined,
      status: filters.status, category: filters.category,
    })
  } finally {
    loading.value = false
  }
}

async function loadChildren() {
  if (!canWrite.value) return
  const result = await api.get<PageResult<ChildView>>('/children', { page: 1, size: 100, status: 'ACTIVE' })
  children.value = result.items
}

function create() {
  editing.value = null
  formOpen.value = true
}

function edit(record: AidView) {
  editing.value = record
  formOpen.value = true
}

async function save(payload: AidFormPayload) {
  saving.value = true
  try {
    if (editing.value) await api.put<AidView>(`/aid-requests/${editing.value.id}`, payload)
    else await api.post<AidView>('/aid-requests', payload)
    message.success(editing.value ? '需求已更新' : '需求已创建')
    formOpen.value = false
    await load(data.value.page)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadChildren()])
})
</script>

<template>
  <div class="page-stack">
    <PageHeader title="帮扶需求" subtitle="需求审核通过后，仅公开经过确认的脱敏摘要。">
      <template #actions><a-button v-if="canWrite" type="primary" :disabled="!children.length" @click="create"><PlusOutlined /> 新建需求</a-button></template>
    </PageHeader>
    <a-alert v-if="canWrite && !children.length" type="info" show-icon message="需要先有审核通过的儿童档案，才能创建帮扶需求。" />
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-input v-model:value="filters.keyword" allow-clear placeholder="需求编号或标题" @press-enter="load(1)" />
          <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 160px" :options="['DRAFT','PENDING_REVIEW','APPROVED','REJECTED','MATCHED','IN_PROGRESS','PENDING_ACCEPTANCE','COMPLETED','CLOSED'].map(value => ({ value, label: statusLabel(value) }))" />
          <a-select v-model:value="filters.category" allow-clear placeholder="全部类型" style="width: 150px" :options="categoryOptions" />
          <a-button type="primary" @click="load(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="data.items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 1080 }">
        <a-table-column title="需求编号" data-index="requestNo" :width="180" />
        <a-table-column title="需求标题" data-index="title" :width="220" />
        <a-table-column title="儿童档案" data-index="childFileNo" :width="180" />
        <a-table-column title="类型" :width="130"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
        <a-table-column title="优先级" :width="90"><template #default="{ record }">{{ priorityLabel(record.priority) }}</template></a-table-column>
        <a-table-column title="负责人" data-index="creatorName" :width="120" />
        <a-table-column title="状态" :width="120"><template #default="{ record }"><StatusTag :status="record.status" /></template></a-table-column>
        <a-table-column title="更新时间" data-index="updatedAt" :width="180" />
        <a-table-column title="操作" :width="150" fixed="right">
          <template #default="{ record }">
            <a-space>
              <RouterLink :to="`/app/aid-requests/${record.id}`">详情</RouterLink>
              <a-button v-if="canWrite && ['DRAFT','REJECTED'].includes(record.status)" type="link" @click="edit(record)">编辑</a-button>
            </a-space>
          </template>
        </a-table-column>
        <template #emptyText><a-empty description="暂无帮扶需求" /></template>
      </a-table>
      <a-pagination v-if="data.total > data.size" class="pagination" :current="data.page" :page-size="data.size" :total="data.total" :show-size-changer="false" @change="load" />
    </div>
    <AidFormModal v-model:open="formOpen" :record="editing" :children="children" :loading="saving" @submit="save" />
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}
</style>
