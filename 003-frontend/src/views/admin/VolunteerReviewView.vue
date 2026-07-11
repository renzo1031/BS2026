<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import type { PageResult, VolunteerView } from '@/types/models'
import type { ReviewPayload } from '@/types/forms'
import { statusLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import ReviewModal from '@/components/common/ReviewModal.vue'

const loading = shallowRef(false)
const reviewing = shallowRef(false)
const reviewOpen = shallowRef(false)
const current = shallowRef<VolunteerView | null>(null)
const status = shallowRef<string | undefined>('PENDING_REVIEW')
const data = shallowRef<PageResult<VolunteerView>>({ items: [], total: 0, page: 1, size: 10 })

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<VolunteerView>>('/admin/volunteers', {
      page, size: data.value.size, status: status.value,
    })
  } finally {
    loading.value = false
  }
}

function openReview(record: VolunteerView) {
  current.value = record
  reviewOpen.value = true
}

async function review(payload: ReviewPayload) {
  if (!current.value) return
  reviewing.value = true
  try {
    await api.post<void>(`/admin/volunteers/${current.value.userId}/review`, payload)
    message.success('志愿者认证已审核')
    reviewOpen.value = false
    await load(data.value.page)
  } finally {
    reviewing.value = false
  }
}

onMounted(() => load())
</script>

<template>
  <div class="page-stack">
    <PageHeader title="志愿者审核" subtitle="审核通过后，志愿者才能申请公开帮扶需求。" />
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-select v-model:value="status" allow-clear placeholder="全部状态" style="width: 180px" :options="['UNVERIFIED','PENDING_REVIEW','APPROVED','REJECTED','SUSPENDED'].map(value => ({ value, label: statusLabel(value) }))" />
          <a-button type="primary" @click="load(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="data.items" :loading="loading" row-key="userId" :pagination="false" :scroll="{ x: 1100 }">
        <a-table-column title="账号" data-index="username" :width="130" />
        <a-table-column title="真实姓名" data-index="realName" :width="120" />
        <a-table-column title="联系电话" data-index="phone" :width="140" />
        <a-table-column title="服务区域" data-index="serviceRegion" :width="160" />
        <a-table-column title="服务技能" data-index="skills" :width="220" :ellipsis="true" />
        <a-table-column title="可服务时间" data-index="availableTime" :width="180" :ellipsis="true" />
        <a-table-column title="状态" :width="120"><template #default="{ record }"><StatusTag :status="record.certificationStatus" /></template></a-table-column>
        <a-table-column title="操作" :width="100" fixed="right"><template #default="{ record }"><a-button v-if="record.certificationStatus === 'PENDING_REVIEW'" type="primary" size="small" @click="openReview(record)">审核</a-button></template></a-table-column>
        <template #emptyText><a-empty description="暂无志愿者认证记录" /></template>
      </a-table>
      <a-pagination v-if="data.total > data.size" class="pagination" :current="data.page" :page-size="data.size" :total="data.total" :show-size-changer="false" @change="load" />
    </div>
    <ReviewModal v-model:open="reviewOpen" title="审核志愿者认证" :loading="reviewing" @submit="review" />
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}
</style>
