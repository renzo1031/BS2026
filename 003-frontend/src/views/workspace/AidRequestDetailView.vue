<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons-vue'
import { Grid, message } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { AidView, ApplicationView, ChildView, PageResult } from '@/types/models'
import type { AidFormPayload, ReviewPayload } from '@/types/forms'
import { categoryLabel, priorityLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import ReviewModal from '@/components/common/ReviewModal.vue'
import AidFormModal from '@/components/aid/AidFormModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const detail = shallowRef<AidView | null>(null)
const applications = shallowRef<ApplicationView[]>([])
const children = shallowRef<ChildView[]>([])
const reviewOpen = shallowRef(false)
const formOpen = shallowRef(false)
const cancelOpen = shallowRef(false)
const cancelReason = shallowRef('')
const canWrite = computed(() => auth.hasPermission('aid:write'))
const canReview = computed(() => auth.hasPermission('aid:review'))
const canMatch = computed(() => auth.hasPermission('application:manage'))
const canCancel = computed(() => auth.hasPermission('aid:write') || auth.hasPermission('aid:review'))
const screens = Grid.useBreakpoint()
const descriptionColumns = computed(() => screens.value.lg ? 3 : screens.value.sm ? 2 : 1)
const titleSpan = computed(() => Math.min(descriptionColumns.value, 2))
const descriptionTailSpan = computed(() => descriptionColumns.value === 2 ? 2 : 1)

async function load() {
  loading.value = true
  try {
    detail.value = await api.get<AidView>(`/aid-requests/${route.params.id}`)
    const tasks: Promise<unknown>[] = []
    if (canWrite.value) tasks.push(api.get<PageResult<ChildView>>('/children', { page: 1, size: 100, status: 'ACTIVE' }).then(result => { children.value = result.items }))
    if (canMatch.value && detail.value.status === 'APPROVED') tasks.push(api.get<ApplicationView[]>(`/aid-requests/${detail.value.id}/applications`).then(result => { applications.value = result }))
    await Promise.all(tasks)
  } finally {
    loading.value = false
  }
}

async function save(payload: AidFormPayload) {
  if (!detail.value) return
  actionLoading.value = true
  try {
    detail.value = await api.put<AidView>(`/aid-requests/${detail.value.id}`, payload)
    formOpen.value = false
    message.success('需求已更新')
  } finally {
    actionLoading.value = false
  }
}

async function submitReview() {
  if (!detail.value) return
  await api.post<void>(`/aid-requests/${detail.value.id}/submit`, { version: detail.value.version })
  message.success('已提交审核')
  await load()
}

async function review(payload: ReviewPayload) {
  if (!detail.value) return
  actionLoading.value = true
  try {
    await api.post<void>(`/aid-requests/${detail.value.id}/review`, { ...payload, version: detail.value.version })
    message.success('审核已完成')
    reviewOpen.value = false
    await load()
  } finally {
    actionLoading.value = false
  }
}

async function accept(application: ApplicationView) {
  if (!detail.value) return
  const assignmentId = await api.post<number>(`/applications/${application.id}/accept`, { requestVersion: detail.value.version })
  message.success('匹配成功，已生成服务任务')
  await router.push(`/app/assignments/${assignmentId}`)
}

async function cancelRequest() {
  if (!detail.value || !cancelReason.value.trim()) return
  actionLoading.value = true
  try {
    await api.post<void>(`/aid-requests/${detail.value.id}/cancel`, {
      version: detail.value.version,
      reason: cancelReason.value.trim(),
    })
    message.success('需求已取消')
    cancelOpen.value = false
    cancelReason.value = ''
    await load()
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <a-button type="text" class="back-button" @click="router.push('/app/aid-requests')"><ArrowLeftOutlined /> 返回需求列表</a-button>
    <PageHeader :title="detail?.requestNo ?? '需求详情'" :subtitle="detail?.title">
      <template #actions>
        <StatusTag v-if="detail" :status="detail.status" />
        <a-button v-if="detail && canWrite && ['DRAFT','REJECTED'].includes(detail.status)" @click="formOpen = true"><EditOutlined /> 编辑</a-button>
        <a-button v-if="detail && canWrite && ['DRAFT','REJECTED'].includes(detail.status)" type="primary" @click="submitReview">提交审核</a-button>
        <a-button v-if="detail?.status === 'PENDING_REVIEW' && canReview" type="primary" @click="reviewOpen = true">审核</a-button>
        <a-button v-if="detail && canCancel && ['DRAFT','PENDING_REVIEW','APPROVED','MATCHED'].includes(detail.status)" danger @click="cancelOpen = true">取消需求</a-button>
      </template>
    </PageHeader>
    <a-spin :spinning="loading">
      <section v-if="detail" class="detail-section surface">
        <a-alert v-if="detail.status === 'REJECTED' && detail.rejectionReason" type="error" show-icon :message="detail.rejectionReason" />
        <a-descriptions bordered :column="descriptionColumns" class="descriptions">
          <a-descriptions-item label="需求标题" :span="titleSpan">{{ detail.title }}</a-descriptions-item>
          <a-descriptions-item label="状态"><StatusTag :status="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="儿童档案">{{ detail.childFileNo }} · {{ detail.childName }}</a-descriptions-item>
          <a-descriptions-item label="服务类型">{{ categoryLabel(detail.category) }}</a-descriptions-item>
          <a-descriptions-item label="优先级">{{ priorityLabel(detail.priority) }}</a-descriptions-item>
          <a-descriptions-item label="负责部门">{{ detail.departmentName }}</a-descriptions-item>
          <a-descriptions-item label="负责人">{{ detail.creatorName }}</a-descriptions-item>
          <a-descriptions-item label="版本" :span="descriptionTailSpan">{{ detail.version }}</a-descriptions-item>
          <a-descriptions-item label="内部情况说明" :span="descriptionColumns">{{ detail.description }}</a-descriptions-item>
          <a-descriptions-item label="公开摘要" :span="descriptionColumns">{{ detail.publicSummary }}</a-descriptions-item>
        </a-descriptions>
      </section>
      <section v-if="detail?.status === 'APPROVED' && canMatch" class="applications-section surface">
        <h2>志愿申请</h2>
        <a-table :data-source="applications" row-key="id" :pagination="false" :scroll="{ x: 760 }">
          <a-table-column title="志愿者" data-index="volunteerName" :width="140" />
          <a-table-column title="申请说明" data-index="message" />
          <a-table-column title="状态" :width="110"><template #default="{ record }"><StatusTag :status="record.status" /></template></a-table-column>
          <a-table-column title="申请时间" data-index="createdAt" :width="180" />
          <a-table-column title="操作" :width="100"><template #default="{ record }"><a-button v-if="record.status === 'APPLIED'" type="primary" size="small" @click="accept(record)">接受</a-button></template></a-table-column>
          <template #emptyText><a-empty description="暂无志愿申请" /></template>
        </a-table>
      </section>
    </a-spin>
    <AidFormModal v-if="detail" v-model:open="formOpen" :record="detail" :children="children" :loading="actionLoading" @submit="save" />
    <ReviewModal v-if="detail" v-model:open="reviewOpen" title="审核帮扶需求" :version="detail.version" :loading="actionLoading" @submit="review" />
    <a-modal v-model:open="cancelOpen" title="取消帮扶需求" :confirm-loading="actionLoading" @ok="cancelRequest">
      <a-form layout="vertical">
        <a-form-item label="取消原因" required>
          <a-textarea v-model:value="cancelReason" :rows="4" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.back-button {
  width: fit-content;
  padding-left: 0;
}

.detail-section,
.applications-section {
  padding: 20px;
}

.descriptions {
  margin-top: 16px;
}

.applications-section h2 {
  margin: 0 0 16px;
  font-size: 18px;
}
</style>
