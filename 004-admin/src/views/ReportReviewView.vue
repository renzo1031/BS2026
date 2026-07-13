<script setup lang="ts">
import { onMounted, shallowRef, useTemplateRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import { useAuthStore } from '@/stores/auth'
import type { PageResult, ReportReview } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import ReportReviewList from '@/components/reviews/ReportReviewList.vue'

const auth = useAuthStore()
const listRef = useTemplateRef<InstanceType<typeof ReportReviewList>>('list')
const busy = shallowRef(false)
const pager = usePagedRequest<ReportReview>((page, size) =>
  request<PageResult<ReportReview>>({ url: '/review/reports', params: { page, size } }),
)

async function claim(item: ReportReview) {
  await act(async () => {
    const updated = await request<ReportReview>({ method: 'POST', url: `/review/reports/${item.report.id}/claim`, data: { version: item.report.version } })
    listRef.value?.update(updated)
    ElMessage.success('案件已认领')
  })
}

async function decide(payload: { item: ReportReview; actioned: boolean; actionType?: string; resolution: string; durationHours?: number }) {
  await act(async () => {
    await request({ method: 'POST', url: `/review/reports/${payload.item.report.id}/decision`, data: {
      version: payload.item.report.version,
      actioned: payload.actioned,
      actionType: payload.actionType,
      resolution: payload.resolution,
      durationHours: payload.durationHours,
    } })
    ElMessage.success(payload.actioned ? '案件已处置' : '举报已驳回')
    listRef.value?.close()
    await pager.load()
  })
}

async function resolveAppeal(payload: { item: ReportReview; uphold: boolean; resolution: string }) {
  await act(async () => {
    await request({ method: 'POST', url: `/admin/reports/${payload.item.report.id}/appeal-decision`, data: {
      version: payload.item.report.version,
      uphold: payload.uphold,
      resolution: payload.resolution,
    } })
    ElMessage.success(payload.uphold ? '已维持原处置' : '已撤销原处置')
    listRef.value?.close()
    await pager.load()
  })
}

async function act(action: () => Promise<void>) {
  busy.value = true
  try { await action() } catch (cause) { ElMessage.error(errorMessage(cause)) } finally { busy.value = false }
}

onMounted(pager.load)
</script>

<template>
  <div class="page">
    <PageHeader title="举报与申诉" description="核验举报事实并记录处置依据，申诉仅由平台管理员复核。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新队列</ElButton></template>
    </PageHeader>
    <section class="page-section review-queue">
      <header class="review-queue__toolbar">
        <div><h2>待处理案件</h2><p>先认领再处置，所有结论都会写入审计记录。</p></div>
        <span>共 {{ pager.total.value }} 项</span>
      </header>
      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="当前没有待处理案件" @retry="pager.load">
        <ReportReviewList
          ref="list"
          :rows="pager.records.value"
          :busy="busy"
          :current-user-id="auth.user?.id"
          :platform-admin="auth.role === 'PLATFORM_ADMIN'"
          @claim="claim"
          @decide="decide"
          @resolve-appeal="resolveAppeal"
        />
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
