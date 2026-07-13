<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import { useAuthStore } from '@/stores/auth'
import type { ActivityMediaSummary, ActivityReviewDetail, ActivityView, PageResult } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import ActivityReviewTable from '@/components/reviews/ActivityReviewTable.vue'
import ActivityReviewDrawer from '@/components/reviews/ActivityReviewDrawer.vue'

const auth = useAuthStore()
const selected = shallowRef<ActivityView | null>(null)
const detail = shallowRef<ActivityReviewDetail | null>(null)
const detailLoading = shallowRef(false)
const detailError = shallowRef('')
const drawerOpen = shallowRef(false)
const busy = shallowRef(false)
let detailRequest = 0
const pager = usePagedRequest<ActivityView>((page, size) =>
  request<PageResult<ActivityView>>({ url: '/review/activities', params: { page, size } }),
)

function openDetail(activity: ActivityView) {
  selected.value = activity
  detail.value = null
  detailError.value = ''
  drawerOpen.value = true
  void loadDetail(activity)
}

async function loadDetail(activity = selected.value) {
  if (!activity) return
  const requestId = ++detailRequest
  detailLoading.value = true
  detailError.value = ''
  try {
    const result = await request<ActivityReviewDetail>({ url: `/review/activities/${activity.id}` })
    if (requestId !== detailRequest) return
    detail.value = result
    selected.value = result.activity
  } catch (cause) {
    if (requestId === detailRequest) detailError.value = errorMessage(cause)
  } finally {
    if (requestId === detailRequest) detailLoading.value = false
  }
}

async function claim(activity: ActivityView) {
  await act(async () => {
    const updated = await request<ActivityView>({
      method: 'POST',
      url: `/review/activities/${activity.id}/claim`,
      data: { version: activity.version },
    })
    selected.value = updated
    if (detail.value) detail.value = { ...detail.value, activity: updated }
    ElMessage.success('已认领，请在 15 分钟内完成审核')
    void loadDetail(updated)
  })
}

async function openMedia(media: ActivityMediaSummary) {
  try {
    const result = await request<{ url: string }>({ url: `/files/${media.id}/url` })
    window.open(result.url, '_blank', 'noopener,noreferrer')
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  }
}

async function decide(payload: { activity: ActivityView; approve: boolean; reason: string }) {
  await act(async () => {
    await request<ActivityView>({
      method: 'POST',
      url: `/review/activities/${payload.activity.id}/decision`,
      data: { version: payload.activity.version, approve: payload.approve, reason: payload.reason || undefined },
    })
    ElMessage.success(payload.approve ? '活动已通过并发布' : '活动已驳回')
    drawerOpen.value = false
    await pager.load()
  })
}

async function act(action: () => Promise<void>) {
  busy.value = true
  try {
    await action()
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
    <PageHeader title="活动审核" description="核对活动内容、参与规则和安全风险，通过后活动才会公开。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新队列</ElButton></template>
    </PageHeader>
    <section class="page-section review-queue">
      <header class="review-queue__toolbar">
        <div><h2>待审核队列</h2><p>按提交顺序处理，点击一行进入证据审查。</p></div>
        <span>共 {{ pager.total.value }} 项</span>
      </header>
      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="当前没有待审核活动" @retry="pager.load">
        <ActivityReviewTable :rows="pager.records.value" @select="openDetail" />
        <ElPagination
          class="pagination"
          :current-page="pager.page.value"
          :page-size="pager.size.value"
          :total="pager.total.value"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="pager.changePage"
          @size-change="pager.changeSize"
        />
      </AsyncState>
    </section>
    <ActivityReviewDrawer
      v-model="drawerOpen"
      :activity="selected"
      :queue-rows="pager.records.value"
      :detail="detail"
      :detail-loading="detailLoading"
      :detail-error="detailError"
      :busy="busy"
      :current-user-id="auth.user?.id"
      @claim="claim"
      @decide="decide"
      @open-media="openMedia"
      @retry-detail="loadDetail()"
      @select-activity="openDetail"
    />
  </div>
</template>

<style scoped>
.review-queue { padding: 0; overflow: hidden; }
.review-queue__toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; background: #fafaf8; border-bottom: 1px solid var(--color-border); }
.review-queue__toolbar h2 { margin: 0; font-size: 14px; }
.review-queue__toolbar p { margin: 4px 0 0; color: var(--color-muted); font-size: 11px; }
.review-queue__toolbar > span { flex: none; color: var(--color-muted); font-size: 12px; }
.pagination { justify-content: flex-end; margin-top: 18px; }
.review-queue :deep(.pagination) { padding: 0 18px 16px; }

@media (max-width: 640px) {
  .review-queue__toolbar { align-items: flex-start; padding: 13px 12px; }
}
</style>
