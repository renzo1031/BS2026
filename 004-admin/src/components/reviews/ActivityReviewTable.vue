<script setup lang="ts">
import { View } from '@element-plus/icons-vue'
import type { ActivityView } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { meetingModeLabels } from './types'

defineProps<{ rows: ActivityView[] }>()
const emit = defineEmits<{ select: [activity: ActivityView] }>()

function select(row: unknown) {
  emit('select', row as ActivityView)
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间待确认'
    : new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}
</script>

<template>
  <div class="queue-table table-wrap">
    <ElTable :data="rows" row-key="id" highlight-current-row @row-click="select">
      <ElTableColumn label="待审活动" min-width="300">
        <template #default="{ row }">
          <button class="activity-link" type="button" :aria-label="`查看活动：${row.title}`" @click.stop="select(row)">
            <span class="activity-link__scene">{{ row.sceneName }}</span>
            <strong>{{ row.title }}</strong>
            <small>{{ formatDate(row.startAt) }} 开始</small>
          </button>
        </template>
      </ElTableColumn>
      <ElTableColumn label="校区" min-width="110">
        <template #default="{ row }">校园 #{{ row.campusId }}</template>
      </ElTableColumn>
      <ElTableColumn label="活动方式" width="120">
        <template #default="{ row }">{{ meetingModeLabels[row.meetingMode] || '其他方式' }}</template>
      </ElTableColumn>
      <ElTableColumn label="提交时间" min-width="150">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </ElTableColumn>
      <ElTableColumn label="名额" width="92">
        <template #default="{ row }">{{ row.acceptedCount }} / {{ row.capacity }}</template>
      </ElTableColumn>
      <ElTableColumn label="审核状态" width="110">
        <template #default="{ row }"><StatusTag :status="row.reviewStatus" /></template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="92" fixed="right">
        <template #default="{ row }">
          <ElButton :icon="View" link type="primary" @click.stop="select(row)">审查</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<style scoped>
.queue-table :deep(.el-table__row) {
  cursor: pointer;
  transition: background-color 0.16s ease;
}

.queue-table :deep(.el-table__cell) {
  padding: 4px 0;
}

.activity-link {
  display: grid;
  gap: 3px;
  width: 100%;
  padding: 8px 0;
  color: inherit;
  text-align: left;
  background: none;
  border: 0;
  cursor: pointer;
}

.activity-link__scene {
  width: max-content;
  padding: 2px 6px;
  color: #176b4e;
  font-size: 11px;
  background: #e8f3ed;
  border-radius: 4px;
}

.activity-link strong {
  overflow: hidden;
  font-size: 14px;
  font-weight: 650;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-link small {
  overflow: hidden;
  color: var(--color-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-link:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .queue-table :deep(.el-table__row) { transition: none; }
}
</style>
