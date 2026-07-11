<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, CheckOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons-vue'
import { Grid, message, Modal } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { AssignmentView } from '@/types/models'
import { categoryLabel } from '@/utils/domain-labels'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const detail = shallowRef<AssignmentView | null>(null)
const visitOpen = shallowRef(false)
const completionOpen = shallowRef(false)
const feedbackOpen = shallowRef(false)
const visitForm = reactive({ serviceDate: '', durationMinutes: 60, content: '', result: '' })
const completionForm = reactive({ summary: '' })
const feedbackForm = reactive({ rating: 5, comment: '' })
const canExecute = computed(() => auth.hasPermission('assignment:execute'))
const canConfirm = computed(() => auth.hasPermission('assignment:confirm'))
const canFeedback = computed(() => auth.hasPermission('feedback:write'))
const screens = Grid.useBreakpoint()
const descriptionColumns = computed(() => screens.value.lg ? 3 : screens.value.sm ? 2 : 1)
const descriptionTailSpan = computed(() => descriptionColumns.value === 2 ? 2 : 1)

async function load() {
  loading.value = true
  try {
    detail.value = await api.get<AssignmentView>(`/assignments/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function start() {
  if (!detail.value) return
  await api.post<void>(`/assignments/${detail.value.id}/start`, { version: detail.value.version })
  message.success('任务已开始')
  await load()
}

async function addVisit() {
  if (!detail.value) return
  actionLoading.value = true
  try {
    await api.post<void>(`/assignments/${detail.value.id}/visits`, visitForm)
    message.success('回访记录已保存')
    visitOpen.value = false
    Object.assign(visitForm, { serviceDate: '', durationMinutes: 60, content: '', result: '' })
    await load()
  } finally {
    actionLoading.value = false
  }
}

async function submitCompletion() {
  if (!detail.value) return
  actionLoading.value = true
  try {
    await api.post<void>(`/assignments/${detail.value.id}/submit-completion`, {
      summary: completionForm.summary,
      version: detail.value.version,
    })
    message.success('已提交验收')
    completionOpen.value = false
    await load()
  } finally {
    actionLoading.value = false
  }
}

function confirmCompletion() {
  if (!detail.value) return
  Modal.confirm({
    title: '确认服务已完成？',
    content: '确认后任务进入已完成状态，等待个案人员评价结案。',
    async onOk() {
      await api.post<void>(`/assignments/${detail.value?.id}/confirm`, { version: detail.value?.version })
      message.success('服务已验收')
      await load()
    },
  })
}

async function submitFeedback() {
  if (!detail.value) return
  actionLoading.value = true
  try {
    await api.post<void>(`/assignments/${detail.value.id}/feedback`, {
      rating: feedbackForm.rating,
      comment: feedbackForm.comment,
      requestVersion: detail.value.requestVersion,
    })
    message.success('评价已提交，需求已结案')
    feedbackOpen.value = false
    await load()
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <a-button type="text" class="back-button" @click="router.push('/app/assignments')"><ArrowLeftOutlined /> 返回任务列表</a-button>
    <PageHeader :title="detail ? `服务任务 #${detail.id}` : '任务详情'" :subtitle="detail?.requestTitle">
      <template #actions>
        <StatusTag v-if="detail" :status="detail.status" />
        <a-button v-if="detail?.status === 'ASSIGNED' && canExecute" type="primary" @click="start"><CheckOutlined /> 开始服务</a-button>
        <a-button v-if="detail?.status === 'IN_PROGRESS' && canExecute" @click="visitOpen = true"><PlusOutlined /> 添加回访</a-button>
        <a-button v-if="detail?.status === 'IN_PROGRESS' && canExecute" type="primary" @click="completionOpen = true"><SendOutlined /> 提交验收</a-button>
        <a-button v-if="detail?.status === 'PENDING_ACCEPTANCE' && canConfirm" type="primary" @click="confirmCompletion">确认完成</a-button>
        <a-button v-if="detail?.status === 'COMPLETED' && canFeedback" type="primary" @click="feedbackOpen = true">服务评价</a-button>
      </template>
    </PageHeader>
    <a-spin :spinning="loading">
      <section v-if="detail" class="detail-section surface">
        <a-descriptions bordered :column="descriptionColumns">
          <a-descriptions-item label="需求编号">{{ detail.requestNo }}</a-descriptions-item>
          <a-descriptions-item label="服务类型">{{ categoryLabel(detail.category) }}</a-descriptions-item>
          <a-descriptions-item label="状态"><StatusTag :status="detail.status" /></a-descriptions-item>
          <a-descriptions-item label="志愿者">{{ detail.volunteerName }}</a-descriptions-item>
          <a-descriptions-item label="儿童档案">{{ detail.childFileNo }}</a-descriptions-item>
          <a-descriptions-item label="儿童姓名">{{ detail.childName }}</a-descriptions-item>
          <a-descriptions-item label="监护人">{{ detail.guardianName }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ detail.guardianPhone }}</a-descriptions-item>
          <a-descriptions-item label="服务区域" :span="descriptionTailSpan">{{ detail.region }}</a-descriptions-item>
          <a-descriptions-item label="联系地址" :span="descriptionColumns">{{ detail.address }}</a-descriptions-item>
          <a-descriptions-item v-if="detail.completionSummary" label="完成总结" :span="descriptionColumns">{{ detail.completionSummary }}</a-descriptions-item>
        </a-descriptions>
      </section>
      <section v-if="detail" class="visits-section surface">
        <div class="section-heading">
          <h2>服务回访</h2>
          <span>{{ detail.visits.length }} 条记录</span>
        </div>
        <a-table :data-source="detail.visits" row-key="id" :pagination="false" :scroll="{ x: 760 }">
          <a-table-column title="服务日期" data-index="serviceDate" :width="130" />
          <a-table-column title="时长" :width="100"><template #default="{ record }">{{ record.durationMinutes }} 分钟</template></a-table-column>
          <a-table-column title="服务内容" data-index="content" :width="260" />
          <a-table-column title="服务结果" data-index="result" :width="240" />
          <a-table-column title="记录人" data-index="creatorName" :width="120" />
          <template #emptyText><a-empty description="暂无回访记录" /></template>
        </a-table>
      </section>
    </a-spin>

    <a-modal v-model:open="visitOpen" title="添加服务回访" :confirm-loading="actionLoading" @ok="addVisit">
      <a-form layout="vertical">
        <a-form-item label="服务日期" required><input v-model="visitForm.serviceDate" class="native-date" type="date" required /></a-form-item>
        <a-form-item label="服务时长（分钟）" required><a-input-number v-model:value="visitForm.durationMinutes" :min="1" :max="1440" style="width: 100%" /></a-form-item>
        <a-form-item label="服务内容" required><a-textarea v-model:value="visitForm.content" :rows="4" :maxlength="1500" show-count /></a-form-item>
        <a-form-item label="服务结果" required><a-textarea v-model:value="visitForm.result" :rows="3" :maxlength="1000" show-count /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="completionOpen" title="提交完成验收" :confirm-loading="actionLoading" @ok="submitCompletion">
      <a-form layout="vertical"><a-form-item label="完成总结" required><a-textarea v-model:value="completionForm.summary" :rows="6" :maxlength="1500" show-count /></a-form-item></a-form>
    </a-modal>

    <a-modal v-model:open="feedbackOpen" title="服务评价" :confirm-loading="actionLoading" @ok="submitFeedback">
      <a-form layout="vertical">
        <a-form-item label="评分" required><a-rate v-model:value="feedbackForm.rating" /></a-form-item>
        <a-form-item label="评价内容" required><a-textarea v-model:value="feedbackForm.comment" :rows="5" :maxlength="1000" show-count /></a-form-item>
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
.visits-section {
  padding: 20px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.section-heading h2 {
  margin: 0;
  font-size: 18px;
}

.section-heading span {
  color: #68746e;
}

.native-date {
  width: 100%;
  height: 38px;
  padding: 4px 11px;
  color: #17212b;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}
</style>
