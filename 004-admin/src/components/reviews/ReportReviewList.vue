<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { Check, Close, Lock, Tickets, User, View, Warning } from '@element-plus/icons-vue'
import type { ReportReview } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { reportReasonLabels, reportTargetLabels } from './types'

const props = defineProps<{ rows: ReportReview[]; busy: boolean; currentUserId?: string; platformAdmin: boolean }>()
const emit = defineEmits<{
  claim: [item: ReportReview]
  decide: [payload: { item: ReportReview; actioned: boolean; actionType?: string; resolution: string; durationHours?: number }]
  resolveAppeal: [payload: { item: ReportReview; uphold: boolean; resolution: string }]
}>()

const open = shallowRef(false)
const selected = shallowRef<ReportReview | null>(null)
const actioned = shallowRef(false)
const actionType = shallowRef('')
const resolution = shallowRef('')
const durationHours = shallowRef<number | undefined>()
const uphold = shallowRef(false)

const claimedByMe = computed(() => selected.value?.assigneeId === props.currentUserId)
const isAppeal = computed(() => selected.value?.report.status === 'APPEALED')
const actionOptions = computed(() => selected.value?.report.targetType === 'ACTIVITY'
  ? [{ label: '下架活动', value: 'REMOVE_ACTIVITY' }]
  : [{ label: '限制账号', value: 'LIMIT_USER' }, { label: '暂停账号', value: 'SUSPEND_USER' }])

watch(actionOptions, (options) => {
  actionType.value = options[0]?.value || ''
}, { immediate: true })

function inspect(row: unknown) {
  const item = row as ReportReview
  selected.value = item
  actioned.value = false
  actionType.value = item.report.targetType === 'ACTIVITY' ? 'REMOVE_ACTIVITY' : 'LIMIT_USER'
  resolution.value = ''
  durationHours.value = undefined
  uphold.value = false
  open.value = true
}

function submit() {
  if (!selected.value) return
  if (isAppeal.value) {
    emit('resolveAppeal', { item: selected.value, uphold: uphold.value, resolution: resolution.value.trim() })
  } else {
    emit('decide', {
      item: selected.value,
      actioned: actioned.value,
      actionType: actioned.value ? actionType.value : undefined,
      resolution: resolution.value.trim(),
      durationHours: actioned.value && selected.value.report.targetType === 'USER' ? durationHours.value : undefined,
    })
  }
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间待确认'
    : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

defineExpose({ close: () => { open.value = false }, update: (item: ReportReview) => { selected.value = item } })
</script>

<template>
  <div class="queue-table table-wrap">
    <ElTable :data="rows" row-key="report.id" highlight-current-row @row-click="inspect">
      <ElTableColumn label="待处理案件" min-width="260">
        <template #default="{ row }">
          <button class="report-link" type="button" :aria-label="`处理案件 ${row.report.id}`" @click.stop="inspect(row)">
            <span class="risk-label">{{ reportReasonLabels[row.report.reasonCode] || '其他违规' }}</span>
            <strong>{{ reportTargetLabels[row.report.targetType] || '其他对象' }} #{{ row.report.targetId }}</strong>
            <small>案件 #{{ row.report.id }}</small>
          </button>
        </template>
      </ElTableColumn>
      <ElTableColumn label="举报说明" prop="report.description" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">{{ row.report.description || '未补充说明' }}</template>
      </ElTableColumn>
      <ElTableColumn label="提交时间" min-width="160">
        <template #default="{ row }">{{ formatDate(row.report.createdAt) }}</template>
      </ElTableColumn>
      <ElTableColumn label="审核状态" width="120"><template #default="{ row }"><StatusTag :status="row.report.status" /></template></ElTableColumn>
      <ElTableColumn label="操作" width="92" fixed="right"><template #default="{ row }"><ElButton :icon="View" link type="primary" @click.stop="inspect(row)">处理</ElButton></template></ElTableColumn>
    </ElTable>
  </div>

  <ElDrawer v-model="open" title="举报与申诉 · 案件审查" size="min(900px, 96vw)" destroy-on-close>
    <article v-if="selected" class="report-workbench">
      <main class="report-main">
        <header class="detail-headline">
          <div>
            <span>{{ reportReasonLabels[selected.report.reasonCode] || '其他违规' }}</span>
            <h2>{{ reportTargetLabels[selected.report.targetType] || '其他对象' }} #{{ selected.report.targetId }}</h2>
            <p>案件 #{{ selected.report.id }} · 提交于 {{ formatDate(selected.report.createdAt) }}</p>
          </div>
          <StatusTag :status="selected.report.status" />
        </header>

        <section class="evidence-section" aria-labelledby="report-evidence-title">
          <div class="section-heading">
            <div><h3 id="report-evidence-title"><ElIcon><Warning /></ElIcon>举报陈述</h3><p>依据举报人提交的事实描述进行核验。</p></div>
          </div>
          <blockquote>{{ selected.report.description || '举报人未补充说明。' }}</blockquote>
        </section>

        <section v-if="selected.report.appealReason" class="appeal-section" aria-labelledby="appeal-title">
          <div class="section-heading">
            <div><h3 id="appeal-title"><ElIcon><Tickets /></ElIcon>申诉理由</h3><p>平台管理员需同时复核原处置事实与申诉内容。</p></div>
          </div>
          <blockquote>{{ selected.report.appealReason }}</blockquote>
        </section>

        <section v-if="selected.report.resolution" class="history-section" aria-labelledby="report-history-title">
          <div class="section-heading">
            <div><h3 id="report-history-title">原处置结论</h3><p>该结论来自前一审核阶段。</p></div>
          </div>
          <p>{{ selected.report.resolution }}</p>
        </section>
      </main>

      <aside class="report-aside" aria-labelledby="report-decision-title">
        <section class="summary-panel">
          <h3>关键信息</h3>
          <dl>
            <div><dt>举报原因</dt><dd>{{ reportReasonLabels[selected.report.reasonCode] || '其他违规' }}</dd></div>
            <div><dt>举报人</dt><dd>用户 #{{ selected.reporterId }}</dd></div>
            <div><dt>校园范围</dt><dd>校园 #{{ selected.report.campusId }}</dd></div>
            <div><dt>被举报对象</dt><dd>{{ reportTargetLabels[selected.report.targetType] || '其他对象' }} #{{ selected.report.targetId }}</dd></div>
          </dl>
        </section>

        <section v-if="isAppeal && platformAdmin" class="decision-panel">
          <div class="decision-panel__heading">
            <h3 id="report-decision-title">申诉复核 <ElIcon><Lock /></ElIcon></h3>
            <p>复核后决定维持或撤销原处置。</p>
          </div>
          <ElRadioGroup v-model="uphold" class="decision-options">
            <ElRadio class="uphold-option" :value="true">维持原处置</ElRadio>
            <ElRadio class="revoke-option" :value="false">撤销原处置</ElRadio>
          </ElRadioGroup>
          <ElInput v-model="resolution" class="decision-input" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="必填：填写申诉复核结论" />
        </section>
        <section v-else-if="selected.report.status === 'REVIEWING' && claimedByMe" class="decision-panel">
          <div class="decision-panel__heading">
            <h3 id="report-decision-title">案件处置 <ElIcon><Lock /></ElIcon></h3>
            <p>记录事实依据，再决定是否执行处置。</p>
          </div>
          <ElRadioGroup v-model="actioned" class="decision-options">
            <ElRadio class="dismiss-option" :value="false">举报不成立</ElRadio>
            <ElRadio class="action-option" :value="true">举报成立并处置</ElRadio>
          </ElRadioGroup>
          <ElSelect v-if="actioned" v-model="actionType" class="decision-input" aria-label="处置动作">
            <ElOption v-for="option in actionOptions" :key="option.value" :label="option.label" :value="option.value" />
          </ElSelect>
          <label v-if="actioned && selected.report.targetType === 'USER'" class="duration-field">
            <span>处置时长（小时）</span>
            <ElInputNumber v-model="durationHours" :min="1" :max="720" placeholder="1 至 720" />
          </label>
          <ElInput v-model="resolution" class="decision-input" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="必填：填写事实依据与处置结论" />
        </section>
        <div v-else class="claim-hint">
          <ElIcon><User /></ElIcon>
          <span>{{ isAppeal ? '申诉仅由平台管理员复核。' : '认领案件后才能提交处置。' }}</span>
        </div>

        <footer class="drawer-footer review-actions">
          <ElButton @click="open = false">关闭</ElButton>
          <ElButton v-if="selected.report.status === 'SUBMITTED' || (selected.report.status === 'REVIEWING' && !claimedByMe)" type="primary" :loading="busy" @click="emit('claim', selected)">认领案件</ElButton>
          <ElButton v-else-if="isAppeal ? platformAdmin : claimedByMe" type="primary" :icon="isAppeal && !uphold ? Close : Check" :disabled="!resolution.trim()" :loading="busy" @click="submit">提交结论</ElButton>
        </footer>
      </aside>
    </article>
  </ElDrawer>
</template>

<style scoped>
.queue-table :deep(.el-table__row) { cursor: pointer; transition: background-color 0.16s ease; }
.queue-table :deep(.el-table__cell) { padding: 4px 0; }

.report-link { display: grid; gap: 3px; width: 100%; padding: 8px 0; color: inherit; text-align: left; background: none; border: 0; cursor: pointer; }
.report-link:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.report-link strong { font-size: 14px; line-height: 1.4; }
.report-link small { color: var(--color-muted); font-size: 11px; }
.risk-label { width: max-content; padding: 2px 6px; color: #a23b32; font-size: 11px; background: #fff0ed; border-radius: 4px; }

.report-workbench { display: grid; grid-template-columns: minmax(0, 1fr) 310px; min-height: calc(100vh - 112px); margin: -20px; background: #f6f6f3; }
.report-main { min-width: 0; padding: 24px; border-right: 1px solid var(--color-border); }

.detail-headline { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.detail-headline span { color: var(--color-danger); font-size: 12px; }
.detail-headline h2 { margin: 7px 0 0; font-size: 23px; line-height: 1.35; }
.detail-headline p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }

.evidence-section,
.appeal-section,
.history-section { margin-top: 16px; padding: 18px; background: #fff; border: 1px solid var(--color-border); border-radius: 6px; animation: section-enter 0.28s ease both; }
.appeal-section { border-left: 3px solid #c98524; animation-delay: 0.04s; }
.history-section { animation-delay: 0.08s; }

.section-heading { margin-bottom: 14px; }
.section-heading h3,
.summary-panel h3,
.decision-panel h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 14px; }
.section-heading p,
.decision-panel p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; line-height: 1.55; }

blockquote { min-height: 96px; padding: 15px; margin: 0; color: #26322d; line-height: 1.75; white-space: pre-wrap; background: #fafaf8; border-left: 3px solid #b14b40; }
.appeal-section blockquote { border-left-color: #c98524; }
.history-section > p { margin: 0; color: #26322d; line-height: 1.75; white-space: pre-wrap; }

.report-aside { display: flex; flex-direction: column; min-width: 0; padding: 20px; background: #fff; }
.summary-panel { padding-bottom: 18px; border-bottom: 1px solid var(--color-border); }
.summary-panel dl { margin: 14px 0 0; }
.summary-panel dl > div + div { margin-top: 11px; }
.summary-panel dt { margin-bottom: 4px; color: var(--color-muted); font-size: 11px; }
.summary-panel dd { margin: 0; color: #26322d; font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }

.decision-panel { position: sticky; top: 0; margin-top: 18px; }
.decision-panel__heading { padding: 13px; background: #f5f5f1; border: 1px solid #e1e2dd; border-radius: 6px; }
.decision-options { display: grid; gap: 6px; margin-top: 14px; }
.decision-options :deep(.el-radio) { height: auto; min-height: 38px; padding: 8px 10px; margin-right: 0; border: 1px solid var(--color-border); border-radius: 5px; }
.decision-options :deep(.uphold-option.is-checked),
.decision-options :deep(.dismiss-option.is-checked) { background: #edf7f1; border-color: #63a584; }
.decision-options :deep(.revoke-option.is-checked),
.decision-options :deep(.action-option.is-checked) { background: #fff1ee; border-color: #d6857d; }
.decision-options :deep(.revoke-option.is-checked .el-radio__label),
.decision-options :deep(.action-option.is-checked .el-radio__label) { color: #a5352d; }
.decision-input { width: 100%; margin-top: 12px; }
.duration-field { display: grid; gap: 6px; margin-top: 12px; color: var(--color-muted); font-size: 11px; }
.duration-field :deep(.el-input-number) { width: 100%; }

.claim-hint { display: flex; gap: 9px; align-items: flex-start; margin-top: 18px; padding: 13px; color: var(--color-muted); font-size: 12px; line-height: 1.55; background: #f4f6f4; border: 1px solid #e1e6e2; border-radius: 6px; }
.claim-hint .el-icon { flex: none; margin-top: 2px; color: var(--color-primary); }

.review-actions { position: sticky; bottom: 0; display: grid; grid-template-columns: auto 1fr; margin-top: auto; padding: 18px 0 0; background: #fff; }
.review-actions .el-button { min-width: 0; margin: 0; }

@keyframes section-enter {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 760px) {
  .report-workbench { display: block; min-height: 0; }
  .report-main { padding: 18px; border-right: 0; }
  .report-aside { padding: 18px; border-top: 1px solid var(--color-border); }
  .decision-panel,
  .review-actions { position: static; }
  .review-actions { margin-top: 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .queue-table :deep(.el-table__row) { transition: none; }
  .evidence-section,
  .appeal-section,
  .history-section { animation: none; }
}
</style>
