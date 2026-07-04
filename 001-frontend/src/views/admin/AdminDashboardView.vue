<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { adminApi } from '../../api/modules'
import type { AdminItemRow, Claim } from '../../types'

const router = useRouter()
const loading = shallowRef(false)
const stats = ref<Record<string, number>>({})
const pendingReviewItems = ref<AdminItemRow[]>([])
const handoverItems = ref<AdminItemRow[]>([])
const auditItems = ref<AdminItemRow[]>([])
const pendingClaims = ref<Claim[]>([])

const metricCards = computed(() => [
  {
    key: 'pendingReview',
    label: '待审核发布',
    value: stats.value.pendingReview ?? 0,
    tone: 'warning',
    path: '/admin/item-review',
    note: '发布内容等待管理员确认'
  },
  {
    key: 'handoverPending',
    label: '待交接物品',
    value: stats.value.handoverPending ?? 0,
    tone: 'primary',
    path: '/admin/handover',
    note: '核验通过后等待线下领取'
  },
  {
    key: 'pendingClaims',
    label: '待核验认领',
    value: stats.value.pendingClaims ?? 0,
    tone: 'danger',
    path: '/admin/claims',
    note: '认领材料需要处理'
  },
  {
    key: 'published',
    label: '前台上架中',
    value: stats.value.published ?? 0,
    tone: 'success',
    path: '/admin/items',
    note: '当前公开可搜索记录'
  },
  {
    key: 'completed',
    label: '已完成闭环',
    value: stats.value.completed ?? 0,
    tone: 'done',
    path: '/admin/items',
    note: '已核验并交接归档前记录'
  },
  {
    key: 'items',
    label: '物品总量',
    value: stats.value.items ?? 0,
    tone: 'neutral',
    path: '/admin/items',
    note: '系统内全部业务记录'
  }
])

const urgentTotal = computed(() =>
  (stats.value.pendingReview ?? 0) + (stats.value.pendingClaims ?? 0) + (stats.value.handoverPending ?? 0)
)
const totalItems = computed(() => stats.value.items ?? 0)
const completedRate = computed(() => percent(stats.value.completed ?? 0, totalItems.value))
const publishedRate = computed(() => percent(stats.value.published ?? 0, totalItems.value))
const todayLabel = computed(() => new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'short'
}).format(new Date()))

const taskQueues = computed(() => [
  {
    label: '发布审核',
    value: stats.value.pendingReview ?? 0,
    helper: '查看是否有虚假、重复或联系方式不完整的登记',
    path: '/admin/item-review',
    action: '去审核'
  },
  {
    label: '认领核验',
    value: stats.value.pendingClaims ?? 0,
    helper: '核对申请人证明材料和物品特征',
    path: '/admin/claims',
    action: '去核验'
  },
  {
    label: '线下交接',
    value: stats.value.handoverPending ?? 0,
    helper: '确认保管位置、领取人和经办记录',
    path: '/admin/handover',
    action: '去交接'
  }
])

function percent(value: number, total: number) {
  if (!total) return 0
  return Math.round((value / total) * 100)
}

function typeLabel(type?: string) {
  return type === 'LOST' ? '寻物' : '招领'
}

function typeFlowLabel(type?: string) {
  return type === 'LOST' ? '丢失登记' : '招领登记'
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    PENDING_REVIEW: '待审核',
    PUBLISHED: '已上架',
    CLAIM_REVIEWING: '认领核验中',
    HANDOVER_PENDING: '待交接',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
    OFFLINE: '已下架'
  }
  return status ? labels[status] || status : '-'
}

function statusTagType(status?: string) {
  const types: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING_REVIEW: 'warning',
    PUBLISHED: 'primary',
    CLAIM_REVIEWING: 'warning',
    HANDOVER_PENDING: 'warning',
    COMPLETED: 'success',
    REJECTED: 'danger',
    OFFLINE: 'info',
    ARCHIVED: 'info'
  }
  return status ? types[status] || 'info' : 'info'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function routeTo(path: string) {
  router.push(path)
}

async function load() {
  loading.value = true
  try {
    const [statsRes, pendingReviewRes, handoverRes, claimsRes, auditRes] = await Promise.all([
      adminApi.statistics(),
      adminApi.items({ status: 'PENDING_REVIEW', pageNum: 1, pageSize: 5 }),
      adminApi.items({ status: 'HANDOVER_PENDING', pageNum: 1, pageSize: 5 }),
      adminApi.claims({ status: 'PENDING', pageNum: 1, pageSize: 5 }),
      adminApi.items({ pageNum: 1, pageSize: 8 })
    ])
    stats.value = statsRes
    pendingReviewItems.value = pendingReviewRes.records
    handoverItems.value = handoverRes.records
    pendingClaims.value = claimsRes.records
    auditItems.value = auditRes.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section v-loading="loading" class="dashboard-page">
    <div class="dashboard-head">
      <div>
        <p class="eyebrow">{{ todayLabel }} · 校园失物招领运营</p>
        <h1 class="section-title">后台首页</h1>
        <p class="head-copy">当前有 {{ urgentTotal }} 项需要处理，优先关注审核、认领核验和线下交接。</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="routeTo('/admin/item-review')">处理审核</el-button>
        <el-button @click="routeTo('/admin/logs')">查看日志</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <button
        v-for="metric in metricCards"
        :key="metric.key"
        class="metric-card"
        :class="`tone-${metric.tone}`"
        type="button"
        @click="routeTo(metric.path)"
      >
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <small>{{ metric.note }}</small>
      </button>
    </div>

    <div class="dashboard-grid">
      <section class="panel work-panel">
        <div class="panel-head">
          <div>
            <h2>待办队列</h2>
            <p>按业务阻塞程度排列</p>
          </div>
        </div>
        <div class="queue-list">
          <div v-for="task in taskQueues" :key="task.label" class="queue-item">
            <div class="queue-main">
              <span>{{ task.label }}</span>
              <strong>{{ task.value }}</strong>
            </div>
            <p>{{ task.helper }}</p>
            <el-button size="small" text type="primary" @click="routeTo(task.path)">{{ task.action }}</el-button>
          </div>
        </div>
      </section>

      <section class="panel health-panel">
        <div class="panel-head">
          <div>
            <h2>业务健康</h2>
            <p>看流程是否堆积</p>
          </div>
        </div>
        <div class="health-row">
          <span>完成闭环率</span>
          <strong>{{ completedRate }}%</strong>
        </div>
        <el-progress :percentage="completedRate" :stroke-width="10" :show-text="false" />
        <div class="health-row">
          <span>前台上架占比</span>
          <strong>{{ publishedRate }}%</strong>
        </div>
        <el-progress :percentage="publishedRate" :stroke-width="10" color="#2f9e73" :show-text="false" />
        <div class="health-note">
          <span>待处理合计</span>
          <strong>{{ urgentTotal }}</strong>
        </div>
      </section>
    </div>

    <div class="table-grid">
      <section class="panel table-panel">
        <div class="panel-head">
          <div>
            <h2>待审核发布</h2>
            <p>优先处理信息不完整或长时间未审核记录</p>
          </div>
          <el-button size="small" @click="routeTo('/admin/item-review')">全部</el-button>
        </div>
        <el-table :data="pendingReviewItems" row-key="id" size="small">
          <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
          <el-table-column label="类型" width="78">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publisherName" label="发布人" width="100" />
          <el-table-column prop="lastOperationTime" label="更新时间" min-width="150" />
        </el-table>
      </section>

      <section class="panel table-panel">
        <div class="panel-head">
          <div>
            <h2>待交接物品</h2>
            <p>核验通过后要确保领取人与经办记录完整</p>
          </div>
          <el-button size="small" @click="routeTo('/admin/handover')">全部</el-button>
        </div>
        <el-table :data="handoverItems" row-key="id" size="small">
          <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
          <el-table-column prop="claimantName" label="认领人" width="100" />
          <el-table-column prop="custodianName" label="保管员" width="100" />
          <el-table-column prop="custodyLocation" label="保管位置" min-width="140" show-overflow-tooltip />
        </el-table>
      </section>
    </div>

    <div class="bottom-grid">
      <section class="panel claim-panel">
        <div class="panel-head">
          <div>
            <h2>认领核验</h2>
            <p>待核验申请 {{ pendingClaims.length }} 条</p>
          </div>
          <el-button size="small" @click="routeTo('/admin/claims')">进入</el-button>
        </div>
        <div v-if="pendingClaims.length" class="claim-list">
          <div v-for="claim in pendingClaims" :key="claim.id" class="claim-item">
            <strong>{{ claim.applicantName }}</strong>
            <span>{{ claim.applicantPhone }}</span>
            <p>{{ claim.proofText }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无待核验认领" />
      </section>

      <section class="panel audit-panel">
        <div class="panel-head">
          <div>
            <h2>物品审计链</h2>
            <p>按最后更新时间追踪发布、认领、审核和经办责任</p>
          </div>
          <div class="panel-actions">
            <el-button size="small" @click="routeTo('/admin/items')">全量物品</el-button>
            <el-button size="small" @click="routeTo('/admin/logs')">操作日志</el-button>
          </div>
        </div>
        <div class="audit-chain-list">
          <article v-for="item in auditItems" :key="item.id" class="audit-chain-card">
            <div class="audit-chain-head">
              <div class="audit-chain-title">
                <strong>{{ item.title }}</strong>
                <span>{{ item.itemNo }}</span>
              </div>
              <el-tag size="small" :type="statusTagType(item.status)" effect="light">{{ statusLabel(item.status) }}</el-tag>
            </div>
            <div class="audit-chain-meta">
              <div>
                <span>发布人</span>
                <strong>{{ item.publisherName || '-' }}</strong>
              </div>
              <div>
                <span>申请人</span>
                <strong>{{ item.claimantName || '-' }}</strong>
              </div>
              <div>
                <span>丢失/领取</span>
                <strong>{{ typeFlowLabel(item.type) }} / {{ item.claimantName ? '已申请领取' : '未领取' }}</strong>
              </div>
              <div>
                <span>审核人</span>
                <strong>{{ item.reviewerName || '-' }}</strong>
              </div>
              <div>
                <span>发布时间</span>
                <strong>{{ formatDateTime(item.createdAt) }}</strong>
              </div>
              <div>
                <span>审核时间</span>
                <strong>{{ formatDateTime(item.reviewTime) }}</strong>
              </div>
              <div>
                <span>最后操作人</span>
                <strong>{{ item.lastOperatorName || '系统' }}</strong>
              </div>
              <div>
                <span>最后更新时间</span>
                <strong>{{ formatDateTime(item.lastOperationTime || item.updatedAt) }}</strong>
              </div>
            </div>
            <div class="audit-chain-foot">
              <span>最后操作</span>
              <strong>{{ item.lastOperationSummary || '暂无操作摘要' }}</strong>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--brand-deep);
  font-size: 13px;
  font-weight: 700;
}

.head-copy {
  margin: 0;
  color: var(--muted);
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 132px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--line);
  border-top: 4px solid var(--brand);
  border-radius: 8px;
  color: #1f2a37;
  cursor: pointer;
}

.metric-card span,
.metric-card small {
  display: block;
}

.metric-card span {
  color: var(--muted);
  font-weight: 700;
}

.metric-card strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 34px;
  line-height: 1;
}

.metric-card small {
  color: var(--muted);
  line-height: 1.5;
}

.tone-warning {
  border-top-color: #f7b731;
}

.tone-primary {
  border-top-color: var(--brand);
}

.tone-danger {
  border-top-color: #d92d20;
}

.tone-success,
.tone-done {
  border-top-color: var(--green);
}

.tone-neutral {
  border-top-color: #667085;
}

.dashboard-grid,
.table-grid,
.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.table-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.bottom-grid {
  grid-template-columns: minmax(280px, 0.65fr) minmax(0, 1.35fr);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
}

.panel-head p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.queue-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.queue-item {
  padding: 14px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #f8fbff;
}

.queue-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.queue-main span {
  font-weight: 700;
}

.queue-main strong {
  color: var(--brand);
  font-size: 28px;
}

.queue-item p {
  min-height: 42px;
  margin: 6px 0 8px;
  color: var(--muted);
  line-height: 1.5;
}

.health-row,
.health-note {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 10px 0 8px;
}

.health-note {
  margin-top: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f2f7fc;
}

.health-row strong,
.health-note strong {
  color: var(--brand);
}

.table-panel {
  overflow: hidden;
}

.claim-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.claim-item {
  padding: 12px;
  border: 1px solid #e6edf5;
  border-radius: 8px;
  background: #fff;
}

.claim-item strong,
.claim-item span {
  margin-right: 10px;
}

.claim-item span {
  color: var(--muted);
}

.claim-item p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.5;
}

.audit-panel {
  min-width: 0;
  overflow: hidden;
}

.audit-chain-list {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.audit-chain-card {
  padding: 14px;
  border: 1px solid #dce9f5;
  border-radius: 8px;
  background: linear-gradient(180deg, #fbfdff 0%, #f6faff 100%);
}

.audit-chain-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e6edf5;
}

.audit-chain-title {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.audit-chain-title strong {
  overflow: hidden;
  color: #1f2a37;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-chain-title span,
.audit-chain-meta span,
.audit-chain-foot span {
  color: var(--muted);
  font-size: 12px;
}

.audit-chain-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px 12px;
  padding: 12px 0;
}

.audit-chain-meta div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.audit-chain-meta strong {
  overflow: hidden;
  color: #1f2a37;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-chain-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #eef6ff;
}

.audit-chain-foot strong {
  min-width: 0;
  overflow: hidden;
  color: var(--brand-deep);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid,
  .table-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard-head {
    flex-direction: column;
  }

  .metric-grid,
  .queue-list {
    grid-template-columns: 1fr;
  }

  .audit-chain-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .audit-chain-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
