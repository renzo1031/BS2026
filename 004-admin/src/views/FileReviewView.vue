<script setup lang="ts">
import { onMounted, shallowRef, useTemplateRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { FileView, PageResult } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import FileReviewList from '@/components/reviews/FileReviewList.vue'

const listRef = useTemplateRef<InstanceType<typeof FileReviewList>>('list')
const previewState = shallowRef({ fileId: '', url: '', loading: false, error: '' })
const busy = shallowRef(false)
let previewRequest = 0
const pager = usePagedRequest<FileView>((page, size) =>
  request<PageResult<FileView>>({ url: '/review/files', params: { page, size } }),
)

async function loadPreview(item: FileView) {
  const requestId = ++previewRequest
  previewState.value = { fileId: item.id, url: '', loading: true, error: '' }
  try {
    const result = await request<{ url: string }>({ url: `/files/${item.id}/url` })
    if (requestId !== previewRequest) return
    previewState.value = { fileId: item.id, url: result.url, loading: false, error: '' }
  } catch (cause) {
    if (requestId !== previewRequest) return
    previewState.value = { fileId: item.id, url: '', loading: false, error: errorMessage(cause) }
  }
}

async function decide(payload: { item: FileView; approve: boolean; reason: string }) {
  busy.value = true
  try {
    await request({ method: 'POST', url: `/review/files/${payload.item.id}/decision`, data: { approve: payload.approve, reason: payload.reason || undefined } })
    ElMessage.success(payload.approve ? '图片已通过检查' : '图片已驳回')
    listRef.value?.close()
    await pager.load()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  } finally {
    busy.value = false
  }
}

onMounted(pager.load)
</script>

<template>
  <div class="page">
    <PageHeader title="文件审核" description="技术安全检查已完成，此处核对图片内容与实际业务用途。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新队列</ElButton></template>
    </PageHeader>
    <section class="page-section review-queue">
      <header class="review-queue__toolbar">
        <div><h2>待检查文件</h2><p>打开私有预览，确认内容与头像、活动或证明材料用途一致。</p></div>
        <span>共 {{ pager.total.value }} 项</span>
      </header>
      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="当前没有待检查图片" @retry="pager.load">
        <FileReviewList ref="list" :rows="pager.records.value" :busy="busy" :preview="previewState" @preview="loadPreview" @decide="decide" />
        <ElPagination class="pagination" :current-page="pager.page.value" :page-size="pager.size.value" :total="pager.total.value" layout="total, prev, pager, next" @current-change="pager.changePage" />
      </AsyncState>
    </section>
  </div>
</template>

<style scoped>
.review-queue { padding: 0; overflow: hidden; }
.review-queue__toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; background: #fafaf8; border-bottom: 1px solid var(--color-border); }
.review-queue__toolbar h2 { margin: 0; font-size: 14px; }
.review-queue__toolbar p { margin: 4px 0 0; color: var(--color-muted); font-size: 11px; }
.review-queue__toolbar > span { flex: none; color: var(--color-muted); font-size: 12px; }
.pagination { justify-content: flex-end; margin-top: 18px; padding: 0 18px 16px; }

@media (max-width: 640px) {
  .review-queue__toolbar { align-items: flex-start; padding: 13px 12px; }
}
</style>
