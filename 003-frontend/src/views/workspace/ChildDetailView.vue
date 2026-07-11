<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, EditOutlined } from '@ant-design/icons-vue'
import { Grid, message, Modal } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { ChildView } from '@/types/models'
import type { ChildFormPayload, ReviewPayload } from '@/types/forms'
import { genderLabel, riskLevelLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import ChildFormModal from '@/components/child/ChildFormModal.vue'
import ReviewModal from '@/components/common/ReviewModal.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const detail = shallowRef<ChildView | null>(null)
const formOpen = shallowRef(false)
const reviewOpen = shallowRef(false)
const canWrite = computed(() => auth.hasPermission('child:write'))
const canReview = computed(() => auth.hasPermission('child:review'))
const canArchive = computed(() => auth.hasPermission('child:archive'))
const screens = Grid.useBreakpoint()
const descriptionColumns = computed(() => screens.value.lg ? 3 : screens.value.sm ? 2 : 1)
const descriptionTailSpan = computed(() => descriptionColumns.value === 2 ? 2 : 1)

async function load() {
  loading.value = true
  try {
    detail.value = await api.get<ChildView>(`/children/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function save(payload: ChildFormPayload) {
  if (!detail.value) return
  actionLoading.value = true
  try {
    detail.value = await api.put<ChildView>(`/children/${detail.value.id}`, payload)
    formOpen.value = false
    message.success('档案已更新')
  } finally {
    actionLoading.value = false
  }
}

async function submitReview() {
  if (!detail.value) return
  await api.post<void>(`/children/${detail.value.id}/submit`, { version: detail.value.version })
  message.success('已提交审核')
  await load()
}

async function review(payload: ReviewPayload) {
  if (!detail.value) return
  actionLoading.value = true
  try {
    await api.post<void>(`/children/${detail.value.id}/review`, { ...payload, version: detail.value.version })
    message.success('审核已完成')
    reviewOpen.value = false
    await load()
  } finally {
    actionLoading.value = false
  }
}

function archive() {
  if (!detail.value) return
  Modal.confirm({
    title: '确认归档该儿童档案？',
    content: '只有不存在未结束帮扶需求时才能归档。',
    async onOk() {
      await api.post<void>(`/children/${detail.value?.id}/archive`, { version: detail.value?.version })
      message.success('档案已归档')
      await load()
    },
  })
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <a-button type="text" class="back-button" @click="router.push('/app/children')"><ArrowLeftOutlined /> 返回档案列表</a-button>
    <PageHeader :title="detail?.fileNo ?? '档案详情'" :subtitle="detail ? `${detail.name} · ${detail.departmentName}` : ''">
      <template #actions>
        <StatusTag v-if="detail" :status="detail.status" />
        <a-button v-if="detail && canWrite && ['DRAFT','REJECTED'].includes(detail.status)" @click="formOpen = true"><EditOutlined /> 编辑</a-button>
        <a-button v-if="detail && canWrite && ['DRAFT','REJECTED'].includes(detail.status)" type="primary" @click="submitReview">提交审核</a-button>
        <a-button v-if="detail?.status === 'PENDING_REVIEW' && canReview" type="primary" @click="reviewOpen = true">审核</a-button>
        <a-button v-if="detail?.status === 'ACTIVE' && canArchive" danger @click="archive">归档</a-button>
      </template>
    </PageHeader>
    <a-spin :spinning="loading">
      <section v-if="detail" class="detail-section surface">
        <a-alert v-if="detail.status === 'REJECTED' && detail.rejectionReason" type="error" show-icon :message="detail.rejectionReason" />
        <a-descriptions bordered :column="descriptionColumns" class="descriptions">
          <a-descriptions-item label="姓名">{{ detail.name }}</a-descriptions-item>
          <a-descriptions-item label="性别">{{ genderLabel(detail.gender) }}</a-descriptions-item>
          <a-descriptions-item label="出生日期">{{ detail.birthDate }}</a-descriptions-item>
          <a-descriptions-item label="学段">{{ detail.schoolStage }}</a-descriptions-item>
          <a-descriptions-item label="服务区域">{{ detail.region }}</a-descriptions-item>
          <a-descriptions-item label="风险等级">{{ riskLevelLabel(detail.riskLevel) }}</a-descriptions-item>
          <a-descriptions-item label="监护人">{{ detail.guardianName }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ detail.guardianPhone }}</a-descriptions-item>
          <a-descriptions-item label="负责人" :span="descriptionTailSpan">{{ detail.creatorName }}</a-descriptions-item>
          <a-descriptions-item label="家庭地址" :span="descriptionColumns">{{ detail.address }}</a-descriptions-item>
          <a-descriptions-item label="家庭情况" :span="descriptionColumns">{{ detail.familySummary }}</a-descriptions-item>
        </a-descriptions>
      </section>
    </a-spin>
    <ChildFormModal v-if="detail" v-model:open="formOpen" :record="detail" :loading="actionLoading" @submit="save" />
    <ReviewModal v-if="detail" v-model:open="reviewOpen" title="审核儿童档案" :version="detail.version" :loading="actionLoading" @submit="review" />
  </div>
</template>

<style scoped>
.back-button {
  width: fit-content;
  padding-left: 0;
}

.detail-section {
  padding: 20px;
}

.descriptions {
  margin-top: 16px;
}
</style>
