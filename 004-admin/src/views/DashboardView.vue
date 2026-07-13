<script setup lang="ts">
import { computed, onMounted, shallowRef, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Clock, Document, Refresh, Right, UserFilled, Warning } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import { errorMessage, request } from '@/api/http'
import type { ActivityView, FileView, IdentityReview, PageResult, ReportReview } from '@/types/api'

interface Metric {
  label: string
  caption: string
  value: number
  path: string
  icon: Component
  tone: 'primary' | 'warning' | 'danger' | 'info'
}

const router = useRouter()
const metrics = shallowRef<Metric[]>([])
const loading = shallowRef(false)
const error = shallowRef('')
const total = computed(() => metrics.value.reduce((sum, metric) => sum + metric.value, 0))
const nextQueue = computed(() => [...metrics.value].sort((left, right) => right.value - left.value)[0])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [activities, identities, reports, files] = await Promise.all([
      request<PageResult<ActivityView>>({ url: '/review/activities', params: { page: 1, size: 1 } }),
      request<IdentityReview[]>({ url: '/review/identity-bindings' }),
      request<PageResult<ReportReview>>({ url: '/review/reports', params: { page: 1, size: 1 } }),
      request<PageResult<FileView>>({ url: '/review/files', params: { page: 1, size: 1 } }),
    ])
    metrics.value = [
      { label: '活动审核', caption: '发布前内容与规则核验', value: Number(activities.total), path: '/reviews/activities', icon: Calendar, tone: 'primary' },
      { label: '身份审核', caption: '校园标识与证明材料', value: identities.length, path: '/reviews/identities', icon: UserFilled, tone: 'warning' },
      { label: '举报与申诉', caption: '处置和平台复核', value: Number(reports.total), path: '/governance/reports', icon: Warning, tone: 'danger' },
      { label: '文件审核', caption: '图片与附件安全检查', value: Number(files.total), path: '/reviews/files', icon: Document, tone: 'info' },
    ]
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

function openNext() {
  if (nextQueue.value) void router.push(nextQueue.value.path)
}

onMounted(load)
</script>

<template>
  <div class="page dashboard">
    <PageHeader title="审核工作台" description="按队列处理校园活动、身份、举报与文件，所有决定自动留痕。">
      <template #actions>
        <ElButton :icon="Refresh" :loading="loading" @click="load">刷新</ElButton>
        <ElButton type="primary" :icon="Right" :disabled="!nextQueue || total === 0" @click="openNext">处理下一条</ElButton>
      </template>
    </PageHeader>

    <AsyncState :loading="loading" :error="error" @retry="load">
      <section class="summary" aria-label="待办概览">
        <div class="summary__lead">
          <span class="summary__icon" aria-hidden="true"><Clock /></span>
          <div><span>全部待处理</span><strong>{{ total }}</strong></div>
        </div>
        <div v-for="metric in metrics" :key="metric.path" class="summary__item">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </section>

      <div class="dashboard-grid">
        <section class="queue-panel" aria-labelledby="queue-title">
          <header class="section-heading">
            <div><h2 id="queue-title">待处理队列</h2><p>优先处理数量较多的队列，进入后仍按提交时间排序。</p></div>
          </header>
          <div class="queue-list">
            <RouterLink v-for="metric in metrics" :key="metric.path" class="queue-row" :to="metric.path">
              <span class="queue-row__icon" :class="`queue-row__icon--${metric.tone}`" aria-hidden="true"><component :is="metric.icon" /></span>
              <span class="queue-row__copy"><strong>{{ metric.label }}</strong><small>{{ metric.caption }}</small></span>
              <span class="queue-row__count">{{ metric.value }}</span>
              <ElIcon class="queue-row__arrow" aria-hidden="true"><Right /></ElIcon>
            </RouterLink>
          </div>
        </section>

        <aside class="review-guide" aria-labelledby="guide-title">
          <header class="section-heading"><div><h2 id="guide-title">处理准则</h2><p>提交决定前完成必要核对。</p></div></header>
          <ol>
            <li><span>1</span><div><strong>确认范围</strong><p>仅处理当前账号授权校园的数据。</p></div></li>
            <li><span>2</span><div><strong>核对证据</strong><p>结合活动信息、证明材料和历史记录判断。</p></div></li>
            <li><span>3</span><div><strong>结论可复核</strong><p>驳回与处置原因应具体、清晰且可执行。</p></div></li>
            <li><span>4</span><div><strong>保护隐私</strong><p>私有证明和举报人信息不得复制或外传。</p></div></li>
          </ol>
        </aside>
      </div>
    </AsyncState>
  </div>
</template>

<style scoped>
.summary { display: grid; grid-template-columns: minmax(220px, 1.25fr) repeat(4, minmax(120px, 1fr)); min-height: 104px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.summary__lead, .summary__item { display: flex; align-items: center; gap: 14px; padding: 18px 22px; }
.summary__lead { border-right: 1px solid var(--color-border); }
.summary__icon { display: grid; width: 42px; height: 42px; place-items: center; color: #fff; background: var(--color-primary); border-radius: 50%; }
.summary__icon :deep(svg) { width: 20px; }
.summary__lead div, .summary__item { display: grid; gap: 6px; }
.summary__lead span, .summary__item span { color: var(--color-muted); font-size: 12px; }
.summary__lead strong { font-size: 28px; line-height: 1; }
.summary__item { position: relative; align-content: center; border-right: 1px solid var(--color-border); }
.summary__item:last-child { border-right: 0; }
.summary__item strong { font-size: 24px; line-height: 1; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, 0.34fr); gap: 16px; margin-top: 16px; }
.queue-panel, .review-guide { min-height: 420px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; padding: 18px 20px; border-bottom: 1px solid var(--color-border); }
.section-heading h2 { margin: 0; font-size: 16px; }
.section-heading p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; }
.queue-list { display: grid; }
.queue-row { display: grid; min-height: 78px; grid-template-columns: 40px minmax(0, 1fr) auto 20px; align-items: center; gap: 14px; padding: 12px 20px; text-decoration: none; border-bottom: 1px solid var(--color-border); transition: background-color 160ms ease, padding-left 160ms ease; }
.queue-row:last-child { border-bottom: 0; }
.queue-row:hover, .queue-row:focus-visible { padding-left: 24px; background: #f4f8f6; outline: none; }
.queue-row__icon { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 5px; }
.queue-row__icon :deep(svg) { width: 18px; }
.queue-row__icon--primary { color: #176b58; background: #e3f1ec; }
.queue-row__icon--warning { color: #916319; background: #fbf0d8; }
.queue-row__icon--danger { color: #a8493f; background: #fae9e6; }
.queue-row__icon--info { color: #4b6884; background: #e8eef4; }
.queue-row__copy { display: grid; gap: 5px; min-width: 0; }
.queue-row__copy strong { font-size: 14px; }
.queue-row__copy small { overflow: hidden; color: var(--color-muted); text-overflow: ellipsis; white-space: nowrap; }
.queue-row__count { min-width: 42px; color: var(--color-ink); font-size: 20px; font-weight: 700; text-align: right; }
.queue-row__arrow { color: #8a9691; }
.review-guide ol { display: grid; margin: 0; padding: 6px 20px 20px; list-style: none; }
.review-guide li { display: grid; grid-template-columns: 28px 1fr; gap: 10px; padding: 16px 0; border-bottom: 1px solid var(--color-border); }
.review-guide li:last-child { border-bottom: 0; }
.review-guide li > span { display: grid; width: 24px; height: 24px; place-items: center; color: var(--color-primary); font-size: 11px; font-weight: 700; background: #e5f0ec; border-radius: 50%; }
.review-guide strong { font-size: 13px; }
.review-guide p { margin: 5px 0 0; color: var(--color-muted); font-size: 12px; line-height: 1.6; }
@media (max-width: 1120px) { .summary { grid-template-columns: repeat(4, 1fr); } .summary__lead { grid-column: 1 / -1; border-right: 0; border-bottom: 1px solid var(--color-border); } .dashboard-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .summary { grid-template-columns: repeat(2, 1fr); margin: 0 12px; } .summary__lead { grid-column: 1 / -1; } .summary__item:nth-child(3) { border-right: 0; } .summary__item { border-bottom: 1px solid var(--color-border); } .summary__item:nth-last-child(-n + 2) { border-bottom: 0; } .dashboard-grid { margin: 12px 0 0; } .queue-panel, .review-guide { border-right: 0; border-left: 0; border-radius: 0; } }
@media (prefers-reduced-motion: reduce) { .queue-row { transition: none; } }
</style>
