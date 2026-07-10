<template>
  <section class="page detail-page">
    <div class="toolbar">
      <h1 class="page-title">申请详情</h1>
      <n-button @click="router.back()">返回</n-button>
    </div>

    <div v-if="loading" class="state-block"><n-spin size="large" /></div>
    <n-result v-else-if="errorMessage" :status="errorResultStatus" :title="errorTitle" :description="errorMessage">
      <template #footer>
        <n-space justify="center">
          <n-button v-if="!['403', '404'].includes(errorResultStatus)" @click="load">重试</n-button>
          <n-button type="primary" @click="router.back()">返回</n-button>
        </n-space>
      </template>
    </n-result>

    <n-card v-else-if="detail" :title="detail.request.title">
      <template #header-extra>
        <n-tag :type="statusType(detail.request.status)" :bordered="false">{{ statusText(detail.request.status) }}</n-tag>
      </template>

      <section aria-labelledby="basic-heading">
        <h2 id="basic-heading" class="section-title">基本信息</h2>
        <n-descriptions bordered :column="descriptionColumns" label-placement="left">
          <n-descriptions-item label="申请编号">{{ detail.request.requestNo }}</n-descriptions-item>
          <n-descriptions-item label="服务事项">{{ detail.item?.name || '-' }}</n-descriptions-item>
          <n-descriptions-item label="事项类型">{{ itemTypeMap[itemType] || itemType || '-' }}</n-descriptions-item>
          <n-descriptions-item label="提交时间">{{ formatDateTime(detail.request.createdAt) }}</n-descriptions-item>
          <n-descriptions-item label="申请人">{{ detail.applicant?.realName || '-' }}</n-descriptions-item>
          <n-descriptions-item label="学号">{{ detail.applicant?.studentNo || '-' }}</n-descriptions-item>
          <n-descriptions-item label="学院">{{ detail.applicant?.college || '-' }}</n-descriptions-item>
          <n-descriptions-item label="联系电话">{{ detail.applicant?.phone || '-' }}</n-descriptions-item>
          <n-descriptions-item label="申请内容" :span="descriptionColumns">{{ detail.request.content }}</n-descriptions-item>
        </n-descriptions>
      </section>

      <section v-if="itemType" aria-labelledby="business-heading">
        <n-divider />
        <h2 id="business-heading" class="section-title">{{ itemTypeMap[itemType] || '业务' }}信息</h2>
        <n-descriptions bordered :column="descriptionColumns" label-placement="left">
          <template v-if="itemType === 'REPAIR'">
            <n-descriptions-item label="故障地点">{{ detail.request.location || '-' }}</n-descriptions-item>
            <n-descriptions-item label="故障类型">{{ fieldText('repairCategory', detail.request.repairCategory) }}</n-descriptions-item>
            <n-descriptions-item label="紧急程度">{{ fieldText('urgency', detail.request.urgency) }}</n-descriptions-item>
          </template>
          <template v-else-if="itemType === 'CERTIFICATE'">
            <n-descriptions-item label="证明类型">{{ fieldText('certificateType', detail.request.certificateType) }}</n-descriptions-item>
            <n-descriptions-item label="语言">{{ fieldText('language', detail.request.language) }}</n-descriptions-item>
            <n-descriptions-item label="份数">{{ detail.request.copies || '-' }}</n-descriptions-item>
            <n-descriptions-item label="领取方式">{{ fieldText('deliveryMethod', detail.request.deliveryMethod) }}</n-descriptions-item>
            <n-descriptions-item label="用途" :span="descriptionColumns">{{ detail.request.purpose || '-' }}</n-descriptions-item>
            <n-descriptions-item v-if="detail.request.certificateNo" label="证明编号">{{ detail.request.certificateNo }}</n-descriptions-item>
            <n-descriptions-item v-if="detail.request.verificationCode" label="校验码">{{ detail.request.verificationCode }}</n-descriptions-item>
          </template>
          <template v-else-if="itemType === 'VENUE'">
            <n-descriptions-item label="活动名称">{{ detail.request.eventName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="活动场地">{{ detail.venue?.name || '-' }}</n-descriptions-item>
            <n-descriptions-item label="场地位置">{{ detail.venue?.location || '-' }}</n-descriptions-item>
            <n-descriptions-item label="场地容量">{{ detail.venue?.capacity ? `${detail.venue.capacity} 人` : '-' }}</n-descriptions-item>
            <n-descriptions-item label="开始时间">{{ formatDateTime(detail.request.appointmentStart) }}</n-descriptions-item>
            <n-descriptions-item label="结束时间">{{ formatDateTime(detail.request.appointmentEnd) }}</n-descriptions-item>
            <n-descriptions-item label="参加人数">{{ detail.request.attendeeCount ? `${detail.request.attendeeCount} 人` : '-' }}</n-descriptions-item>
            <n-descriptions-item label="联系人">{{ detail.request.contactName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="联系电话">{{ detail.request.contactPhone || '-' }}</n-descriptions-item>
          </template>
        </n-descriptions>
      </section>

      <template v-if="detail.request.result">
        <n-divider />
        <section aria-labelledby="result-heading">
          <h2 id="result-heading" class="section-title">处理结果</h2>
          <n-alert type="success" :show-icon="false">{{ detail.request.result }}</n-alert>
        </section>
      </template>

      <template v-if="detail.feedback">
        <n-divider />
        <section aria-labelledby="feedback-heading">
          <h2 id="feedback-heading" class="section-title">服务评价</h2>
          <n-space vertical>
            <n-rate :value="detail.feedback.score" readonly />
            <span>{{ detail.feedback.content || '未填写评价内容' }}</span>
          </n-space>
        </section>
      </template>

      <n-divider />
      <section aria-labelledby="records-heading">
        <h2 id="records-heading" class="section-title">流转记录</h2>
        <n-timeline v-if="detail.records?.length">
          <n-timeline-item
            v-for="record in detail.records"
            :key="record.id"
            :type="statusType(record.toStatus)"
            :title="recordTitle(record)"
            :content="recordContent(record)"
            :time="formatDateTime(record.createdAt)"
          />
        </n-timeline>
        <n-empty v-else description="暂无流转记录" />
      </section>

      <n-divider />
      <n-space class="detail-actions">
        <n-button
          v-if="canCancel"
          type="error"
          secondary
          :loading="cancelLoading"
          @click="cancelRequest"
        >取消申请</n-button>
        <n-button v-if="canFeedback" type="primary" @click="showFeedback = true">评价服务</n-button>
        <n-button
          v-if="canDownloadCertificate"
          secondary
          type="primary"
          :loading="downloadLoading"
          @click="downloadCertificate"
        >下载电子证明</n-button>
        <n-button
          v-if="canAccept"
          type="primary"
          :loading="acceptLoading"
          @click="acceptRequest"
        >受理</n-button>
        <n-button v-if="canReview" type="primary" @click="openProcess('approve')">审核通过</n-button>
        <n-button v-if="canReview" type="error" secondary @click="openProcess('reject')">驳回</n-button>
        <n-button v-if="canFinish" type="success" @click="openProcess('finish')">办结</n-button>
      </n-space>
    </n-card>

    <n-modal v-model:show="showFeedback" preset="card" title="评价服务" style="width: min(440px, calc(100vw - 32px))">
      <n-form ref="feedbackFormRef" :model="feedbackForm" :rules="feedbackRules" @submit.prevent="submitFeedback">
        <n-form-item label="评分" path="score"><n-rate v-model:value="feedbackForm.score" /></n-form-item>
        <n-form-item label="评价内容" path="content">
          <n-input v-model:value="feedbackForm.content" type="textarea" maxlength="500" show-count />
        </n-form-item>
        <div class="modal-actions">
          <n-button @click="showFeedback = false">取消</n-button>
          <n-button attr-type="submit" type="primary" :loading="feedbackLoading">提交评价</n-button>
        </div>
      </n-form>
    </n-modal>

    <n-modal v-model:show="showProcess" preset="card" :title="processTitle" style="width: min(520px, calc(100vw - 32px))">
      <n-form ref="processFormRef" :model="processForm" :rules="processRules" @submit.prevent="submitProcess">
        <n-form-item :label="processLabel" path="comment">
          <n-input
            v-model:value="processForm.comment"
            type="textarea"
            maxlength="1000"
            show-count
            :autosize="{ minRows: 4, maxRows: 8 }"
            :placeholder="`请填写${processLabel}`"
          />
        </n-form-item>
        <div class="modal-actions">
          <n-button @click="showProcess = false">取消</n-button>
          <n-button attr-type="submit" :type="processMode === 'reject' ? 'error' : 'primary'" :loading="processLoading">
            确认{{ processTitle }}
          </n-button>
        </div>
      </n-form>
    </n-modal>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NAlert, NButton, NCard, NDescriptions, NDescriptionsItem, NDivider, NEmpty,
  NForm, NFormItem, NInput, NModal, NRate, NResult, NSpace, NSpin, NTag,
  NTimeline, NTimelineItem, useMessage
} from 'naive-ui'
import http from '../api/http'
import { useAuthStore } from '../store/auth'
import { formatDateTime, itemTypeMap, statusText, statusType } from '../utils/display'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const detail = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const errorStatus = ref(null)
const cancelLoading = ref(false)
const acceptLoading = ref(false)
const downloadLoading = ref(false)
const showFeedback = ref(false)
const feedbackLoading = ref(false)
const feedbackFormRef = ref(null)
const feedbackForm = reactive({ score: 5, content: '' })
const showProcess = ref(false)
const processMode = ref('approve')
const processLoading = ref(false)
const processFormRef = ref(null)
const processForm = reactive({ comment: '' })
const descriptionColumns = ref(2)
let mediaQuery

const feedbackRules = {
  score: { type: 'number', min: 1, max: 5, required: true, message: '请选择 1 至 5 星评分', trigger: 'change' },
  content: { max: 500, message: '评价内容不能超过 500 个字符', trigger: 'input' }
}
const processRules = computed(() => ({
  comment: {
    required: true,
    whitespace: true,
    message: `请填写${processLabel.value}`,
    trigger: ['input', 'blur']
  }
}))

const request = computed(() => detail.value?.request)
const itemType = computed(() => detail.value?.item?.type || request.value?.itemType || '')
const canManage = computed(() => auth.isStaff || auth.isAdmin)
const canCancel = computed(() => auth.isStudent && ['SUBMITTED', 'ACCEPTED'].includes(request.value?.status))
const canFeedback = computed(() => auth.isStudent && request.value?.status === 'FINISHED' && !detail.value?.feedback)
const canDownloadCertificate = computed(() => auth.isStudent && itemType.value === 'CERTIFICATE' && ['FINISHED', 'EVALUATED'].includes(request.value?.status))
const canAccept = computed(() => canManage.value && request.value?.status === 'SUBMITTED')
const canReview = computed(() => canManage.value && request.value?.status === 'ACCEPTED')
const canFinish = computed(() => canManage.value && request.value?.status === 'PROCESSING')
const errorResultStatus = computed(() => errorStatus.value === 403 ? '403' : errorStatus.value === 404 ? '404' : 'error')
const errorTitle = computed(() => errorStatus.value === 403 ? '无权查看该申请' : errorStatus.value === 404 ? '申请不存在' : '申请详情加载失败')
const processTitle = computed(() => ({ approve: '审核通过', reject: '驳回申请', finish: '办结申请' }[processMode.value]))
const processLabel = computed(() => processMode.value === 'finish' ? '办结结果' : processMode.value === 'reject' ? '驳回理由' : '审核意见')

const fieldMaps = {
  repairCategory: { PLUMBING: '水暖设施', ELECTRICAL: '电气设施', FURNITURE: '门窗家具', NETWORK: '网络设备', OTHER: '其他' },
  urgency: { NORMAL: '普通', URGENT: '紧急' },
  certificateType: { ENROLLMENT: '在读证明', STUDENT_STATUS: '学籍证明' },
  language: { CHINESE: '中文', ENGLISH: '英文', BILINGUAL: '中英双语' },
  deliveryMethod: { ONLINE: '在线下载', ON_SITE: '现场领取' }
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 640px)')
  updateColumns(mediaQuery)
  mediaQuery.addEventListener('change', updateColumns)
  load()
})

onBeforeUnmount(() => mediaQuery?.removeEventListener('change', updateColumns))

function updateColumns(event) {
  descriptionColumns.value = event.matches ? 1 : 2
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  errorStatus.value = null
  try {
    detail.value = await http.get(`/requests/${route.params.id}`)
  } catch (error) {
    detail.value = null
    errorMessage.value = error.message
    errorStatus.value = error.status
  } finally {
    loading.value = false
  }
}

function fieldText(field, value) {
  return fieldMaps[field]?.[value] || value || '-'
}

function recordTitle(record) {
  const transition = record.fromStatus
    ? `${statusText(record.fromStatus)} → ${statusText(record.toStatus)}`
    : statusText(record.toStatus)
  return `${record.action || '状态变更'} · ${transition}`
}

function recordContent(record) {
  const operator = record.operatorName ? `操作人：${record.operatorName}` : ''
  return [record.comment, operator].filter(Boolean).join('；') || '无补充说明'
}

async function cancelRequest() {
  cancelLoading.value = true
  try {
    await http.post(`/requests/${route.params.id}/cancel`)
    message.success('申请已取消')
    await load()
  } catch (error) {
    message.error(error.message)
  } finally {
    cancelLoading.value = false
  }
}

async function acceptRequest() {
  acceptLoading.value = true
  try {
    await http.post(`/admin/requests/${route.params.id}/accept`)
    message.success('申请已受理')
    await load()
  } catch (error) {
    message.error(error.message)
  } finally {
    acceptLoading.value = false
  }
}

function openProcess(mode) {
  processMode.value = mode
  processForm.comment = ''
  processFormRef.value?.restoreValidation()
  showProcess.value = true
}

async function submitProcess() {
  try {
    await processFormRef.value?.validate()
  } catch {
    return
  }
  processLoading.value = true
  try {
    if (processMode.value === 'finish') {
      await http.post(`/admin/requests/${route.params.id}/finish`, { result: processForm.comment.trim() })
    } else {
      await http.post(`/admin/requests/${route.params.id}/approve`, {
        approved: processMode.value === 'approve',
        comment: processForm.comment.trim()
      })
    }
    message.success(processTitle.value + '成功')
    showProcess.value = false
    await load()
  } catch (error) {
    message.error(error.message)
  } finally {
    processLoading.value = false
  }
}

async function submitFeedback() {
  try {
    await feedbackFormRef.value?.validate()
  } catch {
    return
  }
  feedbackLoading.value = true
  try {
    await http.post(`/requests/${route.params.id}/feedback`, {
      score: feedbackForm.score,
      content: feedbackForm.content.trim()
    })
    message.success('评价已提交')
    showFeedback.value = false
    await load()
  } catch (error) {
    message.error(error.message)
  } finally {
    feedbackLoading.value = false
  }
}

async function downloadCertificate() {
  downloadLoading.value = true
  try {
    const response = await http.get(`/requests/${route.params.id}/certificate`, { responseType: 'blob' })
    const blob = response.data
    const disposition = response.headers['content-disposition'] || ''
    const encodedName = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
    const plainName = disposition.match(/filename="?([^";]+)"?/i)?.[1]
    const filename = encodedName ? decodeURIComponent(encodedName) : plainName || `${request.value.requestNo}-电子证明.pdf`
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    message.error(error.message)
  } finally {
    downloadLoading.value = false
  }
}
</script>

<style scoped>
.detail-page {
  max-width: 1040px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
}

.detail-actions {
  width: 100%;
}

@media (max-width: 640px) {
  .detail-actions :deep(.n-button) {
    flex: 1 1 140px;
  }
}
</style>
