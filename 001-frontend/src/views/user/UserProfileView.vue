<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi, catalogApi, claimApi, clueApi, itemApi } from '../../api/modules'
import { useAuthStore } from '../../stores/auth'
import ProfileActivityPanel from '../../components/user-profile/ProfileActivityPanel.vue'
import ProfileInfoForm from '../../components/user-profile/ProfileInfoForm.vue'
import ProfileNoticePanel from '../../components/user-profile/ProfileNoticePanel.vue'
import ProfileSecurityPanel from '../../components/user-profile/ProfileSecurityPanel.vue'
import ProfileStats from '../../components/user-profile/ProfileStats.vue'
import ProfileSummary from '../../components/user-profile/ProfileSummary.vue'
import type { Claim, Clue, Item, Notice } from '../../types'

const auth = useAuthStore()
const form = reactive({ realName: '', phone: '', studentNo: '', email: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const loading = shallowRef(false)
const savingProfile = shallowRef(false)
const changingPassword = shallowRef(false)
const overview = reactive({ items: 0, claims: 0, clues: 0, notices: 0 })
const recentItems = shallowRef<Item[]>([])
const recentClaims = shallowRef<Claim[]>([])
const recentClues = shallowRef<Clue[]>([])
const notices = shallowRef<Notice[]>([])

const requiredFields = computed(() => [
  { label: '姓名', value: form.realName },
  { label: '手机号', value: form.phone },
  { label: '学号/工号', value: form.studentNo },
  { label: '邮箱', value: form.email }
])

const completeCount = computed(() => requiredFields.value.filter((field) => Boolean(field.value?.trim())).length)
const completion = computed(() => Math.round((completeCount.value / requiredFields.value.length) * 100))
const missingFields = computed(() => requiredFields.value.filter((field) => !field.value?.trim()).map((field) => field.label))

const quickStats = computed(() => [
  { label: '我的发布', value: overview.items, description: '我登记的寻物和招领记录', to: '/user/items', tone: 'blue' as const },
  { label: '我的认领', value: overview.claims, description: '我提交过的认领申请', to: '/user/claims', tone: 'green' as const },
  { label: '我的线索', value: overview.clues, description: '我反馈过的物品线索', to: '/user/clues', tone: 'amber' as const },
  { label: '校园公告', value: overview.notices, description: '当前可查看的通知公告', to: '/user/notices', tone: 'gray' as const }
])

const itemStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '待审核',
  UNDER_REVIEW: '待审核',
  PUBLISHED: '已上架',
  APPROVED: '已上架',
  REJECTED: '已驳回',
  CLAIM_REVIEWING: '认领核验中',
  CLAIMING: '认领核验中',
  HANDOVER_PENDING: '待交接',
  COMPLETED: '已完成',
  ARCHIVED: '已归档',
  OFFLINE: '已下架'
}

const claimStatusLabels: Record<string, string> = {
  PENDING: '待核验',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  HANDED_OVER: '已交接',
  COMPLETED: '已完成'
}

const clueStatusLabels: Record<string, string> = {
  PENDING_CONFIRM: '待确认',
  VALID: '有效线索',
  INVALID: '无效线索'
}

const recentActivities = computed(() => {
  const itemActivities = recentItems.value.map((item) => ({
    id: `item-${item.id}`,
    title: item.title,
    meta: `${item.type === 'LOST' ? '寻物登记' : '招领登记'} · ${item.lastOperationSummary || '等待下一步处理'}`,
    status: itemStatusLabels[item.status] || item.status,
    time: formatDate(item.updatedAt || item.createdAt || item.lastOperationTime),
    sortTime: toTimestamp(item.updatedAt || item.createdAt || item.lastOperationTime),
    to: '/user/items'
  }))
  const claimActivities = recentClaims.value.map((claim) => ({
    id: `claim-${claim.id}`,
    title: `认领申请 #${claim.id}`,
    meta: claim.proofText || '已提交归属证明材料',
    status: claimStatusLabels[claim.status] || claim.status,
    time: formatDate(claim.createdAt),
    sortTime: toTimestamp(claim.createdAt),
    to: '/user/claims'
  }))
  const clueActivities = recentClues.value.map((clue) => ({
    id: `clue-${clue.id}`,
    title: clue.itemTitle ? `线索反馈：${clue.itemTitle}` : `线索反馈 #${clue.id}`,
    meta: clue.clueContent,
    status: clueStatusLabels[clue.status] || clue.status,
    time: formatDate(clue.createdAt),
    sortTime: toTimestamp(clue.createdAt),
    to: '/user/clues'
  }))

  return [...itemActivities, ...claimActivities, ...clueActivities]
    .sort((a, b) => b.sortTime - a.sortTime)
    .slice(0, 6)
})

watch(
  () => auth.user,
  (user) => {
    if (!user) return
    form.realName = user.realName
    form.phone = user.phone
    form.studentNo = user.studentNo || ''
    form.email = user.email || ''
  },
  { immediate: true }
)

async function saveProfile() {
  savingProfile.value = true
  try {
    auth.user = await authApi.updateProfile(form)
    ElMessage.success('个人资料已保存')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!passwordForm.oldPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('请填写原密码，并确保新密码不少于 6 位')
    return
  }
  changingPassword.value = true
  try {
    await authApi.changePassword(passwordForm)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    ElMessage.success('密码已修改')
  } finally {
    changingPassword.value = false
  }
}

async function loadOverview() {
  loading.value = true
  const [itemsResult, claimsResult, cluesResult, noticesResult] = await Promise.allSettled([
    itemApi.mine({ pageNum: 1, pageSize: 3 }),
    claimApi.mine({ pageNum: 1, pageSize: 3 }),
    clueApi.mine({ pageNum: 1, pageSize: 3 }),
    catalogApi.notices()
  ])

  if (itemsResult.status === 'fulfilled') {
    overview.items = itemsResult.value.total
    recentItems.value = itemsResult.value.records
  }
  if (claimsResult.status === 'fulfilled') {
    overview.claims = claimsResult.value.total
    recentClaims.value = claimsResult.value.records
  }
  if (cluesResult.status === 'fulfilled') {
    overview.clues = cluesResult.value.total
    recentClues.value = cluesResult.value.records
  }
  if (noticesResult.status === 'fulfilled') {
    notices.value = noticesResult.value
    overview.notices = noticesResult.value.length
  }

  if ([itemsResult, claimsResult, cluesResult, noticesResult].some((result) => result.status === 'rejected')) {
    ElMessage.warning('个人中心部分数据加载失败，请稍后刷新')
  }
  loading.value = false
}

function formatDate(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

function toTimestamp(value?: string) {
  if (!value) return 0
  const parsed = new Date(value).getTime()
  return Number.isNaN(parsed) ? 0 : parsed
}

onMounted(loadOverview)
</script>

<template>
  <section v-loading="loading" class="profile-page">
    <ProfileSummary
      :user="auth.user"
      :roles="auth.roles"
      :completion="completion"
      :complete-count="completeCount"
      :total-count="requiredFields.length"
      :missing-fields="missingFields"
    />

    <ProfileStats :stats="quickStats" />

    <div class="profile-main-grid">
      <ProfileInfoForm
        v-model="form"
        :username="auth.user?.username"
        :roles="auth.roles"
        :saving="savingProfile"
        @save="saveProfile"
      />
      <ProfileSecurityPanel v-model="passwordForm" :changing="changingPassword" @change-password="changePassword" />
    </div>

    <div class="profile-bottom-grid">
      <ProfileActivityPanel :activities="recentActivities" />
      <ProfileNoticePanel :notices="notices" />
    </div>
  </section>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.profile-bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

@media (max-width: 900px) {
  .profile-main-grid,
  .profile-bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
