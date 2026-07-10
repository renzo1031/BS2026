<template>
  <section class="page request-form-page">
    <h1 class="page-title">提交服务申请</h1>

    <div v-if="catalogLoading" class="state-block"><n-spin size="large" /></div>
    <n-result v-else-if="catalogError" status="error" title="申请信息加载失败" :description="catalogError">
      <template #footer><n-button @click="loadCatalog">重试</n-button></template>
    </n-result>
    <n-empty v-else-if="!items.length" description="暂无可办理事项" />

    <n-card v-else>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" @submit.prevent="submit">
        <n-form-item label="服务事项" path="itemId">
          <n-select v-model:value="form.itemId" :options="itemOptions" filterable placeholder="请选择事项" />
        </n-form-item>
        <n-form-item label="申请标题" path="title">
          <n-input v-model:value="form.title" maxlength="100" show-count placeholder="简要说明本次申请" />
        </n-form-item>
        <n-form-item label="申请内容" path="content">
          <n-input
            v-model:value="form.content"
            type="textarea"
            maxlength="1000"
            show-count
            :autosize="{ minRows: 3, maxRows: 7 }"
            placeholder="补充说明申请背景和具体需求"
          />
        </n-form-item>

        <template v-if="selectedType === 'REPAIR'">
          <n-divider title-placement="left">宿舍报修信息</n-divider>
          <div class="form-grid">
            <n-form-item label="故障地点" path="location">
              <n-input v-model:value="form.location" placeholder="例如：3 号宿舍楼 402 室" />
            </n-form-item>
            <n-form-item label="故障类型" path="repairCategory">
              <n-select v-model:value="form.repairCategory" :options="repairCategoryOptions" placeholder="请选择故障类型" />
            </n-form-item>
            <n-form-item label="紧急程度" path="urgency">
              <n-select v-model:value="form.urgency" :options="urgencyOptions" placeholder="请选择紧急程度" />
            </n-form-item>
          </div>
        </template>

        <template v-else-if="selectedType === 'CERTIFICATE'">
          <n-divider title-placement="left">证明办理信息</n-divider>
          <div class="form-grid">
            <n-form-item label="证明类型" path="certificateType">
              <n-select v-model:value="form.certificateType" :options="certificateTypeOptions" placeholder="请选择证明类型" />
            </n-form-item>
            <n-form-item label="语言" path="language">
              <n-select v-model:value="form.language" :options="languageOptions" placeholder="请选择语言" />
            </n-form-item>
            <n-form-item label="份数" path="copies">
              <n-input-number v-model:value="form.copies" :min="1" :max="20" placeholder="请输入份数" />
            </n-form-item>
            <n-form-item label="领取方式" path="deliveryMethod">
              <n-select v-model:value="form.deliveryMethod" :options="deliveryMethodOptions" placeholder="请选择领取方式" />
            </n-form-item>
          </div>
          <n-form-item label="用途" path="purpose">
            <n-input v-model:value="form.purpose" maxlength="300" show-count placeholder="请填写证明用途" />
          </n-form-item>
        </template>

        <template v-else-if="selectedType === 'VENUE'">
          <n-divider title-placement="left">场地预约信息</n-divider>
          <div class="form-grid">
            <n-form-item label="活动名称" path="eventName">
              <n-input v-model:value="form.eventName" maxlength="100" placeholder="请输入活动名称" />
            </n-form-item>
            <n-form-item label="活动场地" path="venueId">
              <n-select v-model:value="form.venueId" :options="venueOptions" filterable placeholder="请选择场地" />
            </n-form-item>
            <n-form-item label="开始时间" path="appointmentStart">
              <n-date-picker
                v-model:formatted-value="form.appointmentStart"
                type="datetime"
                value-format="yyyy-MM-dd'T'HH:mm:ss"
                clearable
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item label="结束时间" path="appointmentEnd">
              <n-date-picker
                v-model:formatted-value="form.appointmentEnd"
                type="datetime"
                value-format="yyyy-MM-dd'T'HH:mm:ss"
                clearable
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item label="参加人数" path="attendeeCount">
              <n-input-number v-model:value="form.attendeeCount" :min="1" placeholder="请输入人数" />
            </n-form-item>
            <n-form-item label="联系人" path="contactName">
              <n-input v-model:value="form.contactName" maxlength="50" placeholder="请输入联系人姓名" />
            </n-form-item>
            <n-form-item label="联系电话" path="contactPhone">
              <n-input v-model:value="form.contactPhone" autocomplete="tel" placeholder="请输入 11 位手机号" />
            </n-form-item>
          </div>
        </template>

        <n-space justify="end">
          <n-button type="default" @click="router.back()">返回</n-button>
          <n-button attr-type="submit" type="primary" :loading="submitting" :disabled="!selectedType">提交申请</n-button>
        </n-space>
      </n-form>
    </n-card>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton, NCard, NDatePicker, NDivider, NEmpty, NForm, NFormItem, NInput,
  NInputNumber, NResult, NSelect, NSpace, NSpin, useMessage
} from 'naive-ui'
import http from '../api/http'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const formRef = ref(null)
const submitting = ref(false)
const catalogLoading = ref(false)
const catalogError = ref('')
const items = ref([])
const venues = ref([])

const form = reactive({
  itemId: null,
  title: '',
  content: '',
  location: '',
  repairCategory: null,
  urgency: null,
  certificateType: null,
  purpose: '',
  language: null,
  copies: 1,
  deliveryMethod: null,
  venueId: null,
  eventName: '',
  appointmentStart: null,
  appointmentEnd: null,
  attendeeCount: null,
  contactName: '',
  contactPhone: ''
})

const repairCategoryOptions = [
  { label: '水暖设施', value: 'PLUMBING' },
  { label: '电气设施', value: 'ELECTRICAL' },
  { label: '门窗家具', value: 'FURNITURE' },
  { label: '网络设备', value: 'NETWORK' },
  { label: '其他', value: 'OTHER' }
]
const urgencyOptions = [
  { label: '普通', value: 'NORMAL' },
  { label: '紧急', value: 'URGENT' }
]
const certificateTypeOptions = [
  { label: '在读证明', value: 'ENROLLMENT' },
  { label: '学籍证明', value: 'STUDENT_STATUS' }
]
const languageOptions = [
  { label: '中文', value: 'CHINESE' },
  { label: '英文', value: 'ENGLISH' },
  { label: '中英双语', value: 'BILINGUAL' }
]
const deliveryMethodOptions = [
  { label: '在线下载', value: 'ONLINE' },
  { label: '现场领取', value: 'ON_SITE' }
]

const itemOptions = computed(() => items.value.map((item) => ({ label: item.name, value: item.id })))
const venueOptions = computed(() => venues.value.map((venue) => ({
  label: `${venue.name}（${venue.location}，容量 ${venue.capacity} 人）`,
  value: venue.id
})))
const selectedItem = computed(() => items.value.find((item) => item.id === form.itemId))
const selectedType = computed(() => selectedItem.value?.type || '')
const selectedVenue = computed(() => venues.value.find((venue) => venue.id === form.venueId))

const required = (message) => ({ required: true, message, trigger: ['change', 'blur', 'input'] })
const requiredNumber = (message) => ({ required: true, type: 'number', message, trigger: ['change', 'blur'] })
const rules = computed(() => {
  const current = {
    itemId: requiredNumber('请选择服务事项'),
    title: [required('请输入申请标题'), { max: 100, message: '标题不能超过 100 个字符', trigger: 'input' }],
    content: [required('请输入申请内容'), { max: 1000, message: '申请内容不能超过 1000 个字符', trigger: 'input' }]
  }
  if (selectedType.value === 'REPAIR') {
    Object.assign(current, {
      location: required('请输入故障地点'),
      repairCategory: required('请选择故障类型'),
      urgency: required('请选择紧急程度')
    })
  }
  if (selectedType.value === 'CERTIFICATE') {
    Object.assign(current, {
      certificateType: required('请选择证明类型'),
      purpose: required('请输入证明用途'),
      language: required('请选择语言'),
      copies: [requiredNumber('请输入份数'), { type: 'number', min: 1, max: 20, message: '份数须为 1 至 20', trigger: ['change', 'blur'] }],
      deliveryMethod: required('请选择领取方式')
    })
  }
  if (selectedType.value === 'VENUE') {
    Object.assign(current, {
      venueId: requiredNumber('请选择活动场地'),
      eventName: required('请输入活动名称'),
      appointmentStart: [
        required('请选择开始时间'),
        { validator: (_, value) => !value || new Date(value).getTime() > Date.now(), message: '开始时间必须晚于当前时间', trigger: ['change', 'blur'] }
      ],
      appointmentEnd: [
        required('请选择结束时间'),
        { validator: (_, value) => !value || !form.appointmentStart || new Date(value) > new Date(form.appointmentStart), message: '结束时间必须晚于开始时间', trigger: ['change', 'blur'] }
      ],
      attendeeCount: [
        requiredNumber('请输入参加人数'),
        {
          validator: (_, value) => !value || !selectedVenue.value || value <= selectedVenue.value.capacity,
          message: () => `参加人数不能超过场地容量 ${selectedVenue.value?.capacity || 0} 人`,
          trigger: ['change', 'blur']
        }
      ],
      contactName: required('请输入联系人姓名'),
      contactPhone: [
        required('请输入联系电话'),
        { pattern: /^1\d{10}$/, message: '请输入 11 位手机号', trigger: ['input', 'blur'] }
      ]
    })
  }
  return current
})

watch(selectedType, async () => {
  await nextTick()
  formRef.value?.restoreValidation()
})

onMounted(loadCatalog)

async function loadCatalog() {
  catalogLoading.value = true
  catalogError.value = ''
  try {
    const [itemData, venueData] = await Promise.all([
      http.get('/catalog/items'),
      http.get('/catalog/venues')
    ])
    items.value = itemData || []
    venues.value = venueData || []
    const queryItemId = Number(route.query.itemId)
    if (queryItemId && items.value.some((item) => item.id === queryItemId)) form.itemId = queryItemId
  } catch (error) {
    catalogError.value = error.message
  } finally {
    catalogLoading.value = false
  }
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  const payload = {
    itemId: form.itemId,
    title: form.title.trim(),
    content: form.content.trim(),
    location: form.location.trim() || null,
    repairCategory: form.repairCategory,
    urgency: form.urgency,
    certificateType: form.certificateType,
    purpose: form.purpose.trim() || null,
    language: form.language,
    copies: form.copies,
    deliveryMethod: form.deliveryMethod,
    venueId: form.venueId,
    eventName: form.eventName.trim() || null,
    appointmentStart: form.appointmentStart,
    appointmentEnd: form.appointmentEnd,
    attendeeCount: form.attendeeCount,
    contactName: form.contactName.trim() || null,
    contactPhone: form.contactPhone.trim() || null
  }
  try {
    await http.post('/requests', payload)
    message.success('申请已提交')
    await router.push('/my-requests')
  } catch (error) {
    message.error(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.request-form-page {
  max-width: 920px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
