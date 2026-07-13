<script setup lang="ts">
import { shallowRef } from 'vue'
import { Check, Close, DocumentChecked, Lock, View } from '@element-plus/icons-vue'
import type { IdentityReview } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { identifierTypeLabels } from './types'

defineProps<{ rows: IdentityReview[]; busy: boolean }>()
const emit = defineEmits<{
  decide: [payload: { item: IdentityReview; approved: boolean; reason: string }]
  openProof: [item: IdentityReview]
}>()

const selected = shallowRef<IdentityReview | null>(null)
const open = shallowRef(false)
const approved = shallowRef(true)
const reason = shallowRef('')

function inspect(row: unknown) {
  selected.value = row as IdentityReview
  approved.value = true
  reason.value = ''
  open.value = true
}

function submit() {
  if (!selected.value) return
  emit('decide', { item: selected.value, approved: approved.value, reason: reason.value.trim() })
}

defineExpose({ close: () => { open.value = false } })
</script>

<template>
  <div class="queue-table table-wrap">
    <ElTable :data="rows" row-key="binding.id" highlight-current-row @row-click="inspect">
      <ElTableColumn label="待核验用户" min-width="210">
        <template #default="{ row }">
          <button class="identity-link" type="button" :aria-label="`核验用户 ${row.binding.userId}`" @click.stop="inspect(row)">
            <strong>用户 #{{ row.binding.userId }}</strong>
            <span>{{ identifierTypeLabels[row.binding.identifierType] || '其他校园标识' }} · {{ row.binding.identifierMasked }}</span>
          </button>
        </template>
      </ElTableColumn>
      <ElTableColumn label="校园范围" min-width="130">
        <template #default="{ row }">校园 #{{ row.binding.campusId }}</template>
      </ElTableColumn>
      <ElTableColumn label="标识类型" min-width="130">
        <template #default="{ row }">{{ identifierTypeLabels[row.binding.identifierType] || '其他校园标识' }}</template>
      </ElTableColumn>
      <ElTableColumn label="证明材料" width="120">
        <template #default="{ row }"><span :class="['proof-state', { 'proof-state--ready': row.binding.proofFileId }]">{{ row.binding.proofFileId ? '已提交' : '未提交' }}</span></template>
      </ElTableColumn>
      <ElTableColumn label="审核状态" width="110">
        <template #default="{ row }"><StatusTag :status="row.binding.status" /></template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="92" fixed="right">
        <template #default="{ row }"><ElButton :icon="View" link type="primary" @click.stop="inspect(row)">核验</ElButton></template>
      </ElTableColumn>
    </ElTable>
  </div>

  <ElDrawer v-model="open" title="校园身份审核 · 证据核验" size="min(820px, 96vw)" destroy-on-close>
    <article v-if="selected" class="identity-workbench">
      <main class="identity-main">
        <header class="detail-headline">
          <div>
            <span>校园身份申请</span>
            <h2>用户 #{{ selected.binding.userId }}</h2>
            <p>校园 #{{ selected.binding.campusId }} · {{ identifierTypeLabels[selected.binding.identifierType] || '其他校园标识' }}</p>
          </div>
          <StatusTag :status="selected.binding.status" />
        </header>

        <section class="evidence-card" aria-labelledby="identity-evidence-title">
          <div class="evidence-card__topline">
            <h3 id="identity-evidence-title"><ElIcon><Lock /></ElIcon>待核验校园标识</h3>
            <span>敏感信息</span>
          </div>
          <strong class="evidence-card__value">{{ selected.identifierPlaintext }}</strong>
          <p>仅限本次审核使用，请勿复制、截图或外传。</p>
        </section>

        <section class="proof-section" aria-labelledby="identity-proof-title">
          <div class="section-heading">
            <div><h3 id="identity-proof-title"><ElIcon><DocumentChecked /></ElIcon>证明材料</h3><p>比对证明材料中的姓名、校园和身份编号。</p></div>
          </div>
          <button v-if="selected.binding.proofFileId" class="proof-file" type="button" @click="emit('openProof', selected)">
            <ElIcon><DocumentChecked /></ElIcon>
            <span><strong>查看私有证明材料</strong><small>仅在新窗口临时打开</small></span>
            <ElIcon><View /></ElIcon>
          </button>
          <div v-else class="proof-empty">申请人未提交证明材料</div>
        </section>
      </main>

      <aside class="identity-aside" aria-labelledby="identity-decision-title">
        <section class="summary-panel">
          <h3>关键信息</h3>
          <dl>
            <div><dt>用户 ID</dt><dd>{{ selected.binding.userId }}</dd></div>
            <div><dt>校园 ID</dt><dd>{{ selected.binding.campusId }}</dd></div>
            <div><dt>标识类型</dt><dd>{{ identifierTypeLabels[selected.binding.identifierType] || '其他校园标识' }}</dd></div>
            <div><dt>脱敏标识</dt><dd>{{ selected.binding.identifierMasked }}</dd></div>
          </dl>
        </section>

        <section class="decision-panel">
          <div class="decision-panel__heading">
            <h3 id="identity-decision-title">处理决定 <ElIcon><Lock /></ElIcon></h3>
            <p>确认校园标识与证明材料属于同一申请人。</p>
          </div>
          <ElRadioGroup v-model="approved" class="decision-options">
            <ElRadio class="approve-option" :value="true">认证通过</ElRadio>
            <ElRadio class="reject-option" :value="false">材料驳回</ElRadio>
          </ElRadioGroup>
          <ElInput v-model="reason" class="decision-reason" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="approved ? '可填写核验说明' : '必填：说明驳回原因'" />
        </section>

        <footer class="drawer-footer review-actions">
          <ElButton @click="open = false">关闭</ElButton>
          <ElButton type="primary" :icon="approved ? Check : Close" :disabled="!approved && !reason.trim()" :loading="busy" @click="submit">提交核验</ElButton>
        </footer>
      </aside>
    </article>
  </ElDrawer>
</template>

<style scoped>
.queue-table :deep(.el-table__row) { cursor: pointer; transition: background-color 0.16s ease; }
.queue-table :deep(.el-table__cell) { padding: 4px 0; }

.identity-link {
  display: grid;
  gap: 4px;
  width: 100%;
  padding: 8px 0;
  color: inherit;
  text-align: left;
  background: none;
  border: 0;
  cursor: pointer;
}

.identity-link strong { font-size: 14px; }
.identity-link span { color: var(--color-muted); font-size: 11px; }
.identity-link:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }

.proof-state { color: var(--color-muted); font-size: 12px; }
.proof-state--ready { color: var(--color-primary); }

.identity-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  min-height: calc(100vh - 112px);
  margin: -20px;
  background: #f6f6f3;
}

.identity-main { min-width: 0; padding: 24px; border-right: 1px solid var(--color-border); }

.detail-headline { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.detail-headline span { color: var(--color-primary); font-size: 12px; }
.detail-headline h2 { margin: 7px 0 0; font-size: 23px; line-height: 1.35; }
.detail-headline p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }

.evidence-card {
  display: grid;
  gap: 14px;
  margin-top: 18px;
  padding: 22px;
  color: #eff6f2;
  background: #173b31;
  border: 1px solid #264d42;
  border-radius: 6px;
  box-shadow: 0 12px 28px rgb(17 48 40 / 14%);
  animation: section-enter 0.28s ease both;
}

.evidence-card__topline { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.evidence-card__topline h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 13px; }
.evidence-card__topline > span { padding: 3px 6px; color: #dcebad; font-size: 10px; background: rgb(255 255 255 / 9%); border-radius: 4px; }
.evidence-card__value { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 26px; line-height: 1.25; overflow-wrap: anywhere; }
.evidence-card p { margin: 0; color: #b7ccc5; font-size: 11px; line-height: 1.55; }

.proof-section { margin-top: 16px; padding: 18px; background: #fff; border: 1px solid var(--color-border); border-radius: 6px; animation: section-enter 0.28s 0.05s ease both; }
.section-heading { margin-bottom: 14px; }
.section-heading h3,
.summary-panel h3,
.decision-panel h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 14px; }
.section-heading p,
.decision-panel p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; line-height: 1.55; }

.proof-file {
  display: grid;
  grid-template-columns: 34px 1fr auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px;
  color: var(--color-ink);
  text-align: left;
  background: #f8faf8;
  border: 1px solid #d9e3dd;
  border-radius: 5px;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.proof-file:hover { background: #f0f6f2; border-color: #7daf99; }
.proof-file:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.proof-file > .el-icon:first-child { width: 34px; height: 34px; color: var(--color-primary); font-size: 19px; background: #e6f1eb; border-radius: 5px; }
.proof-file span { display: grid; gap: 3px; }
.proof-file small { color: var(--color-muted); }
.proof-empty { display: grid; min-height: 88px; place-items: center; color: var(--color-muted); font-size: 12px; background: #f8f9f7; border: 1px dashed #cfd8d2; border-radius: 5px; }

.identity-aside { display: flex; flex-direction: column; min-width: 0; padding: 20px; background: #fff; }
.summary-panel { padding-bottom: 18px; border-bottom: 1px solid var(--color-border); }
.summary-panel dl { margin: 14px 0 0; }
.summary-panel dl > div + div { margin-top: 11px; }
.summary-panel dt { margin-bottom: 4px; color: var(--color-muted); font-size: 11px; }
.summary-panel dd { margin: 0; color: #26322d; font-size: 13px; overflow-wrap: anywhere; }

.decision-panel { position: sticky; top: 0; margin-top: 18px; }
.decision-panel__heading { padding: 13px; background: #f2f6f3; border: 1px solid #dfe8e2; border-radius: 6px; }
.decision-options { display: grid; gap: 6px; margin-top: 14px; }
.decision-options :deep(.el-radio) { height: auto; min-height: 38px; padding: 8px 10px; margin-right: 0; border: 1px solid var(--color-border); border-radius: 5px; }
.decision-options :deep(.approve-option.is-checked) { background: #edf7f1; border-color: #63a584; }
.decision-options :deep(.reject-option.is-checked) { background: #fff1ee; border-color: #d6857d; }
.decision-options :deep(.reject-option.is-checked .el-radio__label) { color: #a5352d; }
.decision-reason { margin-top: 12px; }

.review-actions { position: sticky; bottom: 0; display: grid; grid-template-columns: auto 1fr; margin-top: auto; padding: 18px 0 0; background: #fff; }
.review-actions .el-button { min-width: 0; margin: 0; }

@keyframes section-enter {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 720px) {
  .identity-workbench { display: block; min-height: 0; }
  .identity-main { padding: 18px; border-right: 0; }
  .identity-aside { padding: 18px; border-top: 1px solid var(--color-border); }
  .decision-panel,
  .review-actions { position: static; }
  .review-actions { margin-top: 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .queue-table :deep(.el-table__row),
  .proof-file { transition: none; }
  .evidence-card,
  .proof-section { animation: none; }
}
</style>
