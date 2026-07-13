<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { ActivityMediaSummary, ActivityReviewDetail, ActivityView, PageResult } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import ActivityManagementTable from '@/components/activities/ActivityManagementTable.vue'
import ActivityReviewDrawer from '@/components/reviews/ActivityReviewDrawer.vue'

const filters = reactive({ keyword: '', reviewStatus: '', lifecycleStatus: '', moderationStatus: '', meetingMode: '' })
const selected = shallowRef<ActivityView | null>(null)
const detail = shallowRef<ActivityReviewDetail | null>(null)
const detailLoading = shallowRef(false)
const detailError = shallowRef('')
const drawerOpen = shallowRef(false)
let detailRequest = 0

const pager = usePagedRequest<ActivityView>((page, size) =>
  request<PageResult<ActivityView>>({ url: '/operations/activities', params: query(page, size) }),
  20,
)

function query(page: number, size: number) {
  return {
    page,
    size,
    keyword: filters.keyword.trim() || undefined,
    reviewStatus: filters.reviewStatus || undefined,
    lifecycleStatus: filters.lifecycleStatus || undefined,
    moderationStatus: filters.moderationStatus || undefined,
    meetingMode: filters.meetingMode || undefined,
  }
}

function search() {
  pager.changePage(1)
}

function reset() {
  filters.keyword = ''
  filters.reviewStatus = ''
  filters.lifecycleStatus = ''
  filters.moderationStatus = ''
  filters.meetingMode = ''
  search()
}

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
    const result = await request<ActivityReviewDetail>({ url: `/operations/activities/${activity.id}` })
    if (requestId !== detailRequest) return
    detail.value = result
    selected.value = result.activity
  } catch (cause) {
    if (requestId === detailRequest) detailError.value = errorMessage(cause)
  } finally {
    if (requestId === detailRequest) detailLoading.value = false
  }
}

async function openMedia(media: ActivityMediaSummary) {
  try {
    const result = await request<{ url: string }>({ url: `/files/${media.id}/url` })
    window.open(result.url, '_blank', 'noopener,noreferrer')
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  }
}

onMounted(pager.load)
</script>

<template>
  <div class="page">
    <PageHeader title="活动列表" description="查看管理范围内的全部活动，并追溯从发起、审核、报名到完成与治理的完整链路。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新</ElButton></template>
    </PageHeader>

    <section class="page-section activity-workbench">
      <form class="activity-filters" aria-label="活动筛选" @submit.prevent="search">
        <ElInput v-model="filters.keyword" class="keyword" clearable placeholder="搜索标题、场景或描述" aria-label="关键词" @clear="search" />
        <ElSelect v-model="filters.reviewStatus" clearable placeholder="审核状态" aria-label="审核状态">
          <ElOption label="未提交" value="NOT_SUBMITTED" /><ElOption label="待审核" value="PENDING" /><ElOption label="已通过" value="APPROVED" /><ElOption label="已驳回" value="REJECTED" /><ElOption label="已撤回" value="WITHDRAWN" />
        </ElSelect>
        <ElSelect v-model="filters.lifecycleStatus" clearable placeholder="活动进度" aria-label="活动进度">
          <ElOption label="草稿" value="DRAFT" /><ElOption label="招募中" value="RECRUITING" /><ElOption label="进行中" value="IN_PROGRESS" /><ElOption label="待确认完成" value="COMPLETION_PENDING" /><ElOption label="已完成" value="COMPLETED" /><ElOption label="已取消" value="CANCELLED" /><ElOption label="已过期" value="EXPIRED" />
        </ElSelect>
        <ElSelect v-model="filters.moderationStatus" clearable placeholder="治理状态" aria-label="治理状态">
          <ElOption label="正常" value="NORMAL" /><ElOption label="审查中" value="UNDER_REVIEW" /><ElOption label="已下架" value="REMOVED" />
        </ElSelect>
        <ElSelect v-model="filters.meetingMode" clearable placeholder="活动形式" aria-label="活动形式">
          <ElOption label="线上" value="ONLINE" /><ElOption label="线下" value="OFFLINE" /><ElOption label="混合" value="HYBRID" />
        </ElSelect>
        <div class="filter-actions"><ElButton native-type="submit" type="primary" :icon="Search">查询</ElButton><ElButton @click="reset">重置</ElButton></div>
      </form>

      <div class="result-heading"><div><h2>全部活动</h2><p>点击任一行查看完整链路，本页不会改变活动状态。</p></div><span>共 {{ pager.total.value }} 项</span></div>
      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="当前筛选条件下暂无活动" @retry="pager.load">
        <ActivityManagementTable :rows="pager.records.value" @select="openDetail" />
        <ElPagination class="pagination" :current-page="pager.page.value" :page-size="pager.size.value" :total="pager.total.value" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="pager.changePage" @size-change="pager.changeSize" />
      </AsyncState>
    </section>

    <ActivityReviewDrawer v-model="drawerOpen" readonly :activity="selected" :queue-rows="pager.records.value" :detail="detail" :detail-loading="detailLoading" :detail-error="detailError" :busy="false" @open-media="openMedia" @retry-detail="loadDetail()" @select-activity="openDetail" />
  </div>
</template>

<style scoped>
.activity-workbench { padding: 0; overflow: hidden; }
.activity-filters { display: grid; grid-template-columns: minmax(240px, 1.4fr) repeat(4, minmax(130px, .72fr)) auto; gap: 9px; padding: 15px 18px; background: #fafbf9; border-bottom: 1px solid var(--color-border); }
.filter-actions { display: flex; gap: 8px; }
.filter-actions .el-button { margin: 0; }
.result-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 15px 18px 10px; }
.result-heading h2 { margin: 0; font-size: 14px; }
.result-heading p { margin: 4px 0 0; color: var(--color-muted); font-size: 11px; }
.result-heading > span { flex: none; color: var(--color-muted); font-size: 12px; }
.pagination { justify-content: flex-end; padding: 16px 18px 18px; }
@media (max-width: 1280px) { .activity-filters { grid-template-columns: repeat(3, minmax(160px, 1fr)); } .filter-actions { justify-content: flex-end; } }
@media (max-width: 720px) { .activity-filters { grid-template-columns: 1fr; padding: 13px 12px; } .filter-actions { justify-content: flex-start; } .result-heading { padding: 13px 12px 10px; } .result-heading p { max-width: 260px; } .pagination { justify-content: flex-start; padding-inline: 12px; overflow-x: auto; } }
</style>
