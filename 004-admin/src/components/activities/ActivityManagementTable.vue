<script setup lang="ts">
import { View } from '@element-plus/icons-vue'
import type { ActivityView } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { meetingModeLabels } from '@/components/reviews/types'

defineProps<{ rows: ActivityView[] }>()
const emit = defineEmits<{ select: [activity: ActivityView] }>()

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间待确认'
    : new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

function select(row: unknown) {
  emit('select', row as ActivityView)
}
</script>

<template>
  <div class="activity-table table-wrap">
    <ElTable :data="rows" row-key="id" @row-click="select">
      <ElTableColumn label="活动" min-width="280">
        <template #default="{ row }">
          <button class="activity-link" type="button" :aria-label="`查看活动完整链路：${row.title}`" @click.stop="select(row)">
            <span>{{ row.sceneName }}</span>
            <strong>{{ row.title }}</strong>
            <small>#{{ row.id }} · {{ meetingModeLabels[row.meetingMode] || '其他方式' }}</small>
          </button>
        </template>
      </ElTableColumn>
      <ElTableColumn label="校园" width="110">
        <template #default="{ row }">校园 #{{ row.campusId }}</template>
      </ElTableColumn>
      <ElTableColumn label="审核" width="105">
        <template #default="{ row }"><StatusTag :status="row.reviewStatus" /></template>
      </ElTableColumn>
      <ElTableColumn label="活动进度" width="120">
        <template #default="{ row }"><StatusTag :status="row.lifecycleStatus" /></template>
      </ElTableColumn>
      <ElTableColumn label="治理" width="120">
        <template #default="{ row }"><StatusTag :status="row.moderationStatus" /></template>
      </ElTableColumn>
      <ElTableColumn label="时间 / 名额" min-width="180">
        <template #default="{ row }">
          <div class="schedule"><span>{{ formatDate(row.startAt) }} 开始</span><small>{{ row.acceptedCount }} / {{ row.capacity }} 人</small></div>
        </template>
      </ElTableColumn>
      <ElTableColumn label="创建时间" width="150">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="106" fixed="right">
        <template #default="{ row }"><ElButton link type="primary" :icon="View" @click.stop="select(row)">完整链路</ElButton></template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.activity-table :deep(.el-table__row) { cursor: pointer; }
.activity-table :deep(.el-table__cell) { padding: 4px 0; }
.activity-link { display: grid; gap: 3px; width: 100%; padding: 8px 0; color: inherit; text-align: left; background: transparent; border: 0; cursor: pointer; }
.activity-link > span { width: max-content; padding: 2px 6px; color: #176b4e; font-size: 11px; background: #e8f3ed; border-radius: 4px; }
.activity-link strong { overflow: hidden; font-size: 14px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.activity-link small,
.schedule small { color: var(--color-muted); font-size: 11px; }
.activity-link:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.schedule { display: grid; gap: 3px; }
</style>
