<script setup lang="ts">
import { computed, shallowRef } from 'vue'
import { Check, Close, Lock, Picture, Refresh, View, ZoomIn } from '@element-plus/icons-vue'
import type { FileView } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { fileBusinessTypeLabels } from './types'

interface PreviewState {
  fileId: string
  url: string
  loading: boolean
  error: string
}

const props = defineProps<{ rows: FileView[]; busy: boolean; preview: PreviewState }>()
const emit = defineEmits<{
  preview: [item: FileView]
  decide: [payload: { item: FileView; approve: boolean; reason: string }]
}>()

const selected = shallowRef<FileView | null>(null)
const open = shallowRef(false)
const approve = shallowRef(true)
const reason = shallowRef('')

const previewReady = computed(() => Boolean(selected.value && props.preview.fileId === selected.value.id && props.preview.url))
const previewLoading = computed(() => Boolean(selected.value && props.preview.fileId === selected.value.id && props.preview.loading))
const previewError = computed(() => selected.value && props.preview.fileId === selected.value.id ? props.preview.error : '')

function inspect(row: unknown) {
  const item = row as FileView
  selected.value = item
  approve.value = true
  reason.value = ''
  open.value = true
  emit('preview', item)
}

function submit() {
  if (selected.value) emit('decide', { item: selected.value, approve: approve.value, reason: reason.value.trim() })
}

function formatBytes(value: string) {
  const bytes = Number(value)
  if (!Number.isFinite(bytes) || bytes < 0) return '大小未知'
  return bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

defineExpose({ close: () => { open.value = false } })
</script>

<template>
  <div class="queue-table table-wrap">
    <ElTable :data="rows" row-key="id" highlight-current-row @row-click="inspect">
      <ElTableColumn label="待检查文件" min-width="270">
        <template #default="{ row }">
          <button class="file-link" type="button" :aria-label="`检查文件 ${row.originalName || row.id}`" @click.stop="inspect(row)">
            <span class="file-link__icon"><ElIcon><Picture /></ElIcon></span>
            <span class="file-link__copy"><strong>{{ row.originalName || `文件 #${row.id}` }}</strong><small>{{ fileBusinessTypeLabels[row.businessType] || '其他业务文件' }}</small></span>
          </button>
        </template>
      </ElTableColumn>
      <ElTableColumn label="校园范围" min-width="130"><template #default="{ row }">校园 #{{ row.campusId }}</template></ElTableColumn>
      <ElTableColumn label="图片尺寸" min-width="130"><template #default="{ row }">{{ row.width }} × {{ row.height }}</template></ElTableColumn>
      <ElTableColumn label="文件大小" min-width="110"><template #default="{ row }">{{ formatBytes(row.byteSize) }}</template></ElTableColumn>
      <ElTableColumn label="检查状态" width="110"><template #default="{ row }"><StatusTag :status="row.status" /></template></ElTableColumn>
      <ElTableColumn label="操作" width="92" fixed="right"><template #default="{ row }"><ElButton :icon="View" link type="primary" @click.stop="inspect(row)">检查</ElButton></template></ElTableColumn>
    </ElTable>
  </div>

  <ElDrawer v-model="open" title="文件审核 · 内容检查" size="min(980px, 96vw)" destroy-on-close>
    <article v-if="selected" class="file-workbench">
      <main class="file-main">
        <header class="detail-headline">
          <div>
            <span>{{ fileBusinessTypeLabels[selected.businessType] || '其他业务文件' }}</span>
            <h2>{{ selected.originalName || `文件 #${selected.id}` }}</h2>
            <p>{{ selected.width }} × {{ selected.height }} · {{ formatBytes(selected.byteSize) }}</p>
          </div>
          <StatusTag :status="selected.status" />
        </header>

        <section class="evidence-section" aria-labelledby="file-evidence-title">
          <div class="section-heading">
            <div><h3 id="file-evidence-title"><ElIcon><Picture /></ElIcon>图片证据</h3><p>点击图片可查看原比例大图。</p></div>
            <span v-if="previewReady" class="zoom-hint"><ElIcon><ZoomIn /></ElIcon>点击图片放大</span>
          </div>
          <div class="preview">
            <ElImage v-if="previewReady" :src="preview.url" :preview-src-list="[preview.url]" fit="contain" alt="待审核图片" />
            <div v-else-if="previewLoading" class="preview-state" role="status" aria-live="polite">
              <span class="preview-spinner" aria-hidden="true" />
              <span>正在加载私有预览</span>
            </div>
            <div v-else-if="previewError" class="preview-state preview-state--error" role="alert">
              <strong>预览加载失败</strong>
              <span>{{ previewError }}</span>
              <ElButton :icon="Refresh" size="small" @click="emit('preview', selected)">重新加载</ElButton>
            </div>
            <div v-else class="preview-state"><ElIcon><Picture /></ElIcon><span>暂无可用预览</span></div>
          </div>
        </section>
      </main>

      <aside class="file-aside" aria-labelledby="file-decision-title">
        <section class="summary-panel">
          <h3>文件信息</h3>
          <dl>
            <div><dt>业务用途</dt><dd>{{ fileBusinessTypeLabels[selected.businessType] || '其他业务文件' }}</dd></div>
            <div><dt>校园范围</dt><dd>校园 #{{ selected.campusId }}</dd></div>
            <div><dt>文件类型</dt><dd>{{ selected.contentType }}</dd></div>
            <div><dt>安全重编码</dt><dd>{{ selected.width }} × {{ selected.height }} · {{ formatBytes(selected.byteSize) }}</dd></div>
            <div><dt>文件编号</dt><dd>#{{ selected.id }}</dd></div>
          </dl>
        </section>

        <section class="decision-panel">
          <div class="decision-panel__heading">
            <h3 id="file-decision-title">处理决定 <ElIcon><Lock /></ElIcon></h3>
            <p>核对图片内容是否符合对应业务用途。</p>
          </div>
          <ElRadioGroup v-model="approve" class="decision-options">
            <ElRadio class="approve-option" :value="true">内容通过</ElRadio>
            <ElRadio class="reject-option" :value="false">内容驳回</ElRadio>
          </ElRadioGroup>
          <ElInput v-model="reason" class="decision-reason" type="textarea" :rows="5" maxlength="500" show-word-limit :placeholder="approve ? '可填写检查说明' : '必填：说明不通过原因'" />
        </section>

        <footer class="drawer-footer review-actions">
          <ElButton @click="open = false">关闭</ElButton>
          <ElButton type="primary" :icon="approve ? Check : Close" :disabled="!previewReady || (!approve && !reason.trim())" :loading="busy" @click="submit">提交检查</ElButton>
        </footer>
      </aside>
    </article>
  </ElDrawer>
</template>

<style scoped>
.queue-table :deep(.el-table__row) { cursor: pointer; transition: background-color 0.16s ease; }
.queue-table :deep(.el-table__cell) { padding: 4px 0; }

.file-link { display: grid; grid-template-columns: 34px 1fr; align-items: center; gap: 11px; width: 100%; padding: 8px 0; color: inherit; text-align: left; background: none; border: 0; cursor: pointer; }
.file-link:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.file-link__icon { display: grid; width: 34px; height: 34px; place-items: center; color: var(--color-primary); background: #e8f2ec; border-radius: 5px; }
.file-link__copy { display: grid; min-width: 0; gap: 3px; }
.file-link__copy strong { overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.file-link__copy small { color: var(--color-muted); font-size: 11px; }

.file-workbench { display: grid; grid-template-columns: minmax(0, 1fr) 310px; min-height: calc(100vh - 112px); margin: -20px; background: #f6f6f3; }
.file-main { min-width: 0; padding: 24px; border-right: 1px solid var(--color-border); }

.detail-headline { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-bottom: 20px; border-bottom: 1px solid var(--color-border); }
.detail-headline > div { min-width: 0; }
.detail-headline span { color: var(--color-primary); font-size: 12px; }
.detail-headline h2 { overflow: hidden; margin: 7px 0 0; font-size: 23px; line-height: 1.35; text-overflow: ellipsis; white-space: nowrap; }
.detail-headline p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }

.evidence-section { margin-top: 16px; padding: 18px; background: #fff; border: 1px solid var(--color-border); border-radius: 6px; animation: section-enter 0.28s ease both; }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.section-heading h3,
.summary-panel h3,
.decision-panel h3 { display: flex; align-items: center; gap: 7px; margin: 0; font-size: 14px; }
.section-heading p,
.decision-panel p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; line-height: 1.55; }
.zoom-hint { display: inline-flex; align-items: center; gap: 5px; color: var(--color-primary); font-size: 11px; }

.preview { display: grid; min-height: 430px; place-items: center; overflow: hidden; color: var(--color-muted); background: #ecefeb; border: 1px solid #d4dbd6; border-radius: 6px; }
.preview :deep(.el-image) { width: 100%; height: 430px; cursor: zoom-in; }
.preview-state { display: grid; max-width: 360px; justify-items: center; gap: 10px; padding: 24px; font-size: 12px; text-align: center; }
.preview-state > .el-icon { font-size: 28px; }
.preview-state--error { color: var(--color-danger); }
.preview-spinner { width: 24px; height: 24px; border: 2px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.8s linear infinite; }

.file-aside { display: flex; flex-direction: column; min-width: 0; padding: 20px; background: #fff; }
.summary-panel { padding-bottom: 18px; border-bottom: 1px solid var(--color-border); }
.summary-panel dl { margin: 14px 0 0; }
.summary-panel dl > div + div { margin-top: 11px; }
.summary-panel dt { margin-bottom: 4px; color: var(--color-muted); font-size: 11px; }
.summary-panel dd { margin: 0; color: #26322d; font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }

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

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes section-enter {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 760px) {
  .file-workbench { display: block; min-height: 0; }
  .file-main { padding: 18px; border-right: 0; }
  .file-aside { padding: 18px; border-top: 1px solid var(--color-border); }
  .preview,
  .preview :deep(.el-image) { height: 320px; min-height: 320px; }
  .decision-panel,
  .review-actions { position: static; }
  .review-actions { margin-top: 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .queue-table :deep(.el-table__row) { transition: none; }
  .preview-spinner,
  .evidence-section { animation: none; }
}
</style>
