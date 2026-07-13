<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const labels: Record<string, string> = {
  NOT_SUBMITTED: '未提交',
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  SUBMITTED: '待处理',
  REVIEWING: '处理中',
  ACTIONED: '已处置',
  DISMISSED: '已驳回举报',
  APPEALED: '申诉中',
  UPHELD: '维持处置',
  REVOKED: '已撤销',
  PENDING_SCAN: '待检查',
  ACTIVE: '正常',
  LIMITED: '受限',
  SUSPENDED: '暂停',
  CLOSED: '关闭',
  INACTIVE: '停用',
  UNVERIFIED: '未认证',
  WITHDRAWN: '已撤回',
  DRAFT: '草稿',
  RECRUITING: '招募中',
  IN_PROGRESS: '进行中',
  COMPLETION_PENDING: '待确认完成',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  EXPIRED: '已过期',
  NORMAL: '正常',
  UNDER_REVIEW: '治理审查中',
  REMOVED: '已下架',
}

const tagType = computed<'success' | 'warning' | 'danger' | 'info' | 'primary'>(() => {
  if (['APPROVED', 'ACTIVE', 'REVOKED', 'RECRUITING', 'COMPLETED', 'NORMAL'].includes(props.status)) return 'success'
  if (['REJECTED', 'SUSPENDED', 'CLOSED', 'REMOVED', 'ACTIONED', 'CANCELLED', 'EXPIRED'].includes(props.status)) return 'danger'
  if (['PENDING', 'SUBMITTED', 'REVIEWING', 'APPEALED', 'PENDING_SCAN', 'LIMITED', 'IN_PROGRESS', 'COMPLETION_PENDING', 'UNDER_REVIEW'].includes(props.status)) return 'warning'
  return 'info'
})
</script>

<template>
  <ElTag class="status-tag" :type="tagType" effect="plain" size="small">{{ labels[status] || status }}</ElTag>
</template>

<style scoped>
.status-tag {
  min-width: 48px;
  justify-content: center;
  padding: 0 7px;
  font-size: 11px;
  font-weight: 600;
  line-height: 21px;
  border-radius: 4px;
}
</style>
