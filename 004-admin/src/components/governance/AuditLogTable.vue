<script setup lang="ts">
import type { AuditView } from '@/types/api'

defineProps<{ rows: AuditView[] }>()

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value))
}
</script>

<template>
  <div class="table-wrap">
    <ElTable :data="rows" row-key="id">
      <ElTableColumn type="expand" width="48">
        <template #default="{ row }">
          <section class="audit-detail" :aria-label="`${row.actionName} 审计详情`">
            <dl class="audit-detail__meta">
              <div><dt>校园范围</dt><dd>{{ row.campusId || '全平台' }}</dd></div>
              <div><dt>来源 IP</dt><dd>{{ row.ipAddress || '—' }}</dd></div>
              <div><dt>请求标识</dt><dd>{{ row.requestId || '—' }}</dd></div>
            </dl>
            <div class="audit-detail__states">
              <section><h3>操作前状态</h3><pre>{{ row.beforeState || '—' }}</pre></section>
              <section><h3>操作后状态</h3><pre>{{ row.afterState || '—' }}</pre></section>
            </div>
          </section>
        </template>
      </ElTableColumn>
      <ElTableColumn label="时间" min-width="180"><template #default="{ row }">{{ formatDate(row.createdAt) }}</template></ElTableColumn>
      <ElTableColumn label="操作" prop="actionName" min-width="190" />
      <ElTableColumn label="操作者" min-width="190"><template #default="{ row }">{{ row.operatorRole || 'SYSTEM' }} / {{ row.operatorId || '—' }}</template></ElTableColumn>
      <ElTableColumn label="目标" min-width="210"><template #default="{ row }">{{ row.targetType }} / {{ row.targetId || '—' }}</template></ElTableColumn>
      <ElTableColumn label="理由" prop="reason" min-width="260" show-overflow-tooltip />
    </ElTable>
  </div>
</template>

<style scoped>
.audit-detail { display: grid; gap: 18px; padding: 12px 22px 20px; background: #f7f8f6; }
.audit-detail__meta { display: flex; flex-wrap: wrap; gap: 24px; margin: 0; }
.audit-detail__meta div { display: grid; gap: 4px; }
.audit-detail__meta dt, .audit-detail__states h3 { color: var(--color-muted); font-size: 12px; font-weight: 500; }
.audit-detail__meta dd { margin: 0; }
.audit-detail__states { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.audit-detail__states h3 { margin: 0 0 6px; }
.audit-detail__states pre { min-height: 72px; margin: 0; padding: 12px; overflow-wrap: anywhere; color: #34423d; font: 12px/1.65 ui-monospace, SFMono-Regular, Consolas, monospace; white-space: pre-wrap; background: #fff; border: 1px solid var(--color-border); border-radius: 4px; }
@media (max-width: 720px) { .audit-detail__states { grid-template-columns: 1fr; } }
</style>
