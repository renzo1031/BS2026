<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { Check, Clock, Close, CollectionTag, Document, Location, Lock, User, View } from '@element-plus/icons-vue'
import type { ActivityMediaSummary, ActivityReviewDetail, ActivityView } from '@/types/api'
import StatusTag from '@/components/common/StatusTag.vue'
import { activityActionLabels, meetingModeLabels, workflowStatusLabels } from './types'

const props = defineProps<{
  activity: ActivityView | null
  queueRows: ActivityView[]
  detail?: ActivityReviewDetail | null
  detailLoading?: boolean
  detailError?: string
  busy: boolean
  currentUserId?: string
  readonly?: boolean
}>()
const open = defineModel<boolean>({ required: true })
const emit = defineEmits<{
  claim: [activity: ActivityView]
  decide: [payload: { activity: ActivityView; approve: boolean; reason: string }]
  openMedia: [media: ActivityMediaSummary]
  retryDetail: []
  selectActivity: [activity: ActivityView]
}>()

const decision = shallowRef<'approve' | 'reject'>('approve')
const reason = shallowRef('')
const activeEvidenceTab = shallowRef('media')
const claimedByMe = computed(() => Boolean(props.activity?.reviewerId && props.activity.reviewerId === props.currentUserId))

watch(open, (value) => {
  if (value) {
    decision.value = 'approve'
    reason.value = ''
    activeEvidenceTab.value = 'media'
  }
})

function submitDecision() {
  if (!props.activity) return
  emit('decide', { activity: props.activity, approve: decision.value === 'approve', reason: reason.value.trim() })
}

function formatDate(value?: string) {
  if (!value) return '未提供'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '时间待确认'
    : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

function formatBytes(value?: number) {
  if (!Number.isFinite(value) || (value ?? 0) < 0) return '大小未知'
  const bytes = value ?? 0
  return bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function labelStatus(value?: string) {
  return value ? workflowStatusLabels[value] || '其他状态' : '未记录'
}

function labelAction(value?: string) {
  return value ? activityActionLabels[value] || '系统操作' : '状态变化'
}
</script>

<template>
  <ElDrawer v-model="open" size="calc(100vw - 216px)" :modal="false" :lock-scroll="false" append-to-body destroy-on-close class="review-drawer">
    <template #header>
      <div class="review-drawer-header">
        <button class="back-queue" type="button" @click="open = false"><span aria-hidden="true">‹</span> {{ readonly ? '返回活动列表' : '返回队列' }}</button>
        <span class="review-drawer-header__path">{{ readonly ? '活动档案' : '证据审查' }} <span aria-hidden="true">/</span> {{ readonly ? '完整链路' : '活动审核' }}</span>
      </div>
    </template>
    <article v-if="activity" class="review-workbench">
      <aside class="case-queue" :aria-label="readonly ? '活动列表' : '待审活动队列'">
        <div class="case-queue__toolbar"><span>全部状态</span><span>排序：提交时间</span></div>
        <div class="case-queue__list">
          <button v-for="row in queueRows" :key="row.id" class="case-item" :class="{ 'case-item--active': row.id === activity.id }" type="button" @click="emit('selectActivity', row)">
            <StatusTag :status="row.reviewStatus" />
            <strong>{{ row.title }}</strong>
            <span>{{ row.campusId ? `校园 #${row.campusId}` : '校园范围待载入' }}</span>
            <small>{{ formatDate(row.createdAt) }}</small>
          </button>
        </div>
        <span class="case-queue__count">共 {{ queueRows.length }} 条</span>
      </aside>
      <main class="review-main">
        <header class="review-headline">
          <div class="review-headline__copy">
            <span class="scene-label">{{ activity.sceneName }}</span>
            <h2>{{ activity.title }}</h2>
            <p>活动 #{{ activity.id }} · 校园 #{{ activity.campusId }}</p>
          </div>
          <div class="status-stack" aria-label="活动状态">
            <StatusTag :status="activity.reviewStatus" />
            <StatusTag :status="activity.lifecycleStatus" />
            <StatusTag :status="activity.moderationStatus" />
          </div>
        </header>

        <section class="evidence-section" aria-labelledby="activity-evidence-title">
          <div class="section-heading">
            <div>
              <h3 id="activity-evidence-title"><ElIcon><Document /></ElIcon>活动内容</h3>
              <p>核对标题、描述、地点和参与规则是否一致。</p>
            </div>
            <span class="evidence-count">{{ activity.mediaIds.length }} 份图片素材</span>
          </div>
          <div class="description-panel">{{ activity.description }}</div>
          <div v-if="detailLoading" class="evidence-state" role="status" aria-live="polite">
            <span class="preview-spinner" aria-hidden="true" />正在加载关联证据
          </div>
          <div v-else-if="detailError" class="evidence-state evidence-state--error" role="alert">
            <strong>关联证据加载失败</strong><span>{{ detailError }}</span>
            <ElButton size="small" @click="emit('retryDetail')">重新加载</ElButton>
          </div>
          <ElTabs v-else-if="detail" v-model="activeEvidenceTab" class="evidence-tabs">
            <ElTabPane :label="`活动素材 ${detail.media.length}`" name="media">
              <div v-if="detail.media.length" class="media-list">
                <button v-for="media in detail.media" :key="media.id" class="media-item" type="button" @click="emit('openMedia', media)">
                  <ElIcon><Document /></ElIcon>
                  <span><strong>{{ media.originalName || `素材 #${media.id}` }}</strong><small>{{ media.contentType }} · {{ formatBytes(media.byteSize) }} · {{ media.width && media.height ? `${media.width} × ${media.height}` : '尺寸未记录' }}</small></span>
                  <StatusTag :status="media.status" /><ElIcon><View /></ElIcon>
                </button>
              </div>
              <div v-else class="tab-empty">未关联活动素材</div>
            </ElTabPane>
            <ElTabPane :label="`报名申请 ${detail.applications.length}`" name="applications">
              <div v-if="detail.applications.length" class="application-list">
                <article v-for="item in detail.applications" :key="item.id" class="application-item">
                  <div class="row-between"><div><strong>{{ item.applicant.nickname }}</strong><small>{{ item.applicant.majorName || '专业未提供' }} · {{ item.applicant.gradeName || '年级未提供' }}</small><small>申请于 {{ formatDate(item.createdAt) }}</small></div><span class="state-pill">{{ labelStatus(item.status) }}</span></div>
                  <p v-if="item.message">{{ item.message }}</p>
                  <ol v-if="item.answers.length"><li v-for="answer in item.answers" :key="answer">{{ answer }}</li></ol>
                  <p v-if="item.decisionReason" class="application-reason">处理说明：{{ item.decisionReason }}</p>
                </article>
              </div>
              <div v-else class="tab-empty">暂无报名申请</div>
            </ElTabPane>
            <ElTabPane :label="`参与人员 ${detail.participants.length}`" name="participants">
              <div v-if="detail.participants.length" class="participant-list">
                <div v-for="item in detail.participants" :key="item.user.id" class="participant-item">
                  <span class="participant-avatar"><ElIcon><User /></ElIcon></span>
                  <span class="participant-copy"><strong>{{ item.user.nickname }}</strong><small>{{ workflowStatusLabels[item.memberRole] || '参与成员' }} · {{ item.user.majorName || '专业未提供' }}</small><small>{{ labelStatus(item.status) }} · 完成：{{ labelStatus(item.completionStatus) }} · {{ item.joinedAt ? formatDate(item.joinedAt) : '加入时间未记录' }}</small><small v-if="item.leftAt">退出于 {{ formatDate(item.leftAt) }}</small></span>
                  <span class="state-pill">{{ item.leftAt ? '已退出' : labelStatus(item.status) }}</span>
                </div>
              </div>
              <div v-else class="tab-empty">暂无参与人员</div>
            </ElTabPane>
            <ElTabPane :label="`${readonly ? '完整时间线' : '审核记录'} ${detail.timeline.length}`" name="timeline">
              <div v-if="detail.timeline.length" class="timeline-list">
                <div v-for="(entry, index) in detail.timeline" :key="`${entry.createdAt}-${index}`" class="timeline-item">
                  <span class="timeline-dot" aria-hidden="true" />
                  <div><div class="row-between"><strong>{{ labelAction(entry.actionName) }}</strong><time>{{ formatDate(entry.createdAt) }}</time></div><p>{{ entry.operatorName || '系统' }}<span v-if="entry.operatorRole"> · {{ entry.operatorRole === 'PLATFORM_ADMIN' ? '平台管理员' : entry.operatorRole === 'CAMPUS_REVIEWER' ? '校园审核员' : entry.operatorRole }}</span><span v-if="entry.dimension"> · {{ entry.dimension }}</span><span v-if="entry.fromStatus || entry.toStatus"> · {{ labelStatus(entry.fromStatus) }} 至 {{ labelStatus(entry.toStatus) }}</span></p><small v-if="entry.source || entry.reason">{{ entry.source === 'GOVERNANCE' ? '治理记录' : entry.source === 'AUDIT' ? '审计记录' : '状态记录' }}<span v-if="entry.reason"> · {{ entry.reason }}</span></small></div>
                </div>
              </div>
              <div v-else class="tab-empty">暂无审核记录</div>
            </ElTabPane>
          </ElTabs>
          <div v-if="activity.tags.length" class="tag-list" aria-label="活动标签">
            <ElTag v-for="tag in activity.tags" :key="tag" size="small" effect="plain">{{ tag }}</ElTag>
          </div>
        </section>

        <section class="facts-section" aria-labelledby="activity-facts-title">
          <div class="section-heading">
            <div><h3 id="activity-facts-title">关键信息</h3><p>时间、地点与名额均来自学生提交内容。</p></div>
          </div>
          <dl class="fact-grid">
            <div class="fact-item">
              <dt><ElIcon><Clock /></ElIcon>活动时间</dt>
              <dd>{{ formatDate(activity.startAt) }}<br>至 {{ formatDate(activity.endAt) }}</dd>
            </div>
            <div class="fact-item">
              <dt><ElIcon><Location /></ElIcon>活动地点</dt>
              <dd>{{ activity.publicLocation || (activity.meetingMode === 'ONLINE' ? '线上进行' : '未填写') }}</dd>
            </div>
            <div class="fact-item">
              <dt><ElIcon><User /></ElIcon>参与名额</dt>
              <dd>{{ activity.capacity }} 人，不含发起人</dd>
            </div>
            <div class="fact-item">
              <dt><ElIcon><CollectionTag /></ElIcon>活动方式</dt>
              <dd>{{ meetingModeLabels[activity.meetingMode] || '其他方式' }}</dd>
            </div>
            <div class="fact-item fact-item--wide">
              <dt>申请截止</dt>
              <dd>{{ formatDate(activity.applyDeadline) }}</dd>
            </div>
            <div v-if="activity.memberLocationDetail" class="fact-item fact-item--wide">
              <dt><ElIcon><Lock /></ElIcon>成员可见地点</dt>
              <dd>{{ activity.memberLocationDetail }}</dd>
            </div>
          </dl>
        </section>

        <section class="rules-section" aria-labelledby="activity-rules-title">
          <div class="section-heading">
            <div><h3 id="activity-rules-title">参与规则</h3><p>重点排查歧视性条件、违规引流和不安全要求。</p></div>
          </div>
          <dl class="rules-list">
            <div><dt>加入要求</dt><dd>{{ activity.joinRequirement || '无额外要求' }}</dd></div>
            <div><dt>申请问题</dt><dd><ol v-if="activity.joinQuestions.length"><li v-for="question in activity.joinQuestions" :key="question">{{ question }}</li></ol><span v-else>无申请问题</span></dd></div>
          </dl>
        </section>
      </main>

      <aside class="review-aside" aria-labelledby="activity-decision-title">
        <section class="summary-panel">
          <h3>{{ readonly ? '活动摘要' : '审核摘要' }}</h3>
          <dl>
            <div><dt>提交时间</dt><dd>{{ formatDate(activity.createdAt) }}</dd></div>
            <div><dt>活动校园</dt><dd>{{ detail?.campusName || `校园 #${activity.campusId}` }}</dd></div>
            <div><dt>发起人</dt><dd>{{ detail?.creator.nickname || `用户 #${activity.creatorId}` }}</dd></div>
            <div><dt>审核状态</dt><dd><StatusTag :status="activity.reviewStatus" /></dd></div>
            <div><dt>活动进度</dt><dd><StatusTag :status="activity.lifecycleStatus" /></dd></div>
            <div><dt>治理状态</dt><dd><StatusTag :status="activity.moderationStatus" /></dd></div>
            <div><dt>当前报名</dt><dd>{{ activity.acceptedCount }} / {{ activity.capacity }} 人</dd></div>
            <div><dt>素材数量</dt><dd>{{ detail ? detail.media.length : activity.mediaIds.length }} 份</dd></div>
            <div><dt>最近更新</dt><dd>{{ formatDate(activity.updatedAt || activity.createdAt) }}</dd></div>
            <div v-if="activity.completionDeadlineAt"><dt>完成确认截止</dt><dd>{{ formatDate(activity.completionDeadlineAt) }}</dd></div>
            <div v-if="activity.reviewReason"><dt>审核/治理说明</dt><dd>{{ activity.reviewReason }}</dd></div>
          </dl>
        </section>

        <section v-if="!readonly" class="decision-panel">
          <div class="decision-panel__heading">
            <h3 id="activity-decision-title">处理决定 <ElIcon><Lock /></ElIcon></h3>
            <p v-if="!claimedByMe">先认领任务，避免多人同时处理。</p>
            <p v-else>请依据活动内容和平台规则做出判断。</p>
          </div>
          <template v-if="claimedByMe">
            <ElRadioGroup v-model="decision" class="decision-options">
              <ElRadio class="approve-option" value="approve">通过并发布</ElRadio>
              <ElRadio class="reject-option" value="reject">驳回修改</ElRadio>
            </ElRadioGroup>
            <ElInput
              v-model="reason"
              class="decision-reason"
              :placeholder="decision === 'reject' ? '必填：说明具体修改要求' : '可选：填写审核说明'"
              :rows="4"
              maxlength="500"
              show-word-limit
              type="textarea"
            />
          </template>
        </section>

        <footer class="drawer-footer review-actions" :class="{ 'review-actions--readonly': readonly }">
          <ElButton @click="open = false">关闭</ElButton>
          <ElButton v-if="!readonly && !claimedByMe" type="primary" :loading="busy" @click="emit('claim', activity)">认领任务</ElButton>
          <ElButton
            v-else-if="!readonly"
            type="primary"
            :icon="decision === 'approve' ? Check : Close"
            :disabled="decision === 'reject' && !reason.trim()"
            :loading="busy"
            @click="submitDecision"
          >提交决策</ElButton>
        </footer>
      </aside>
    </article>
  </ElDrawer>
</template>

<style scoped>
.review-workbench {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 300px;
  height: 100%;
  background: #f6f6f3;
  overflow: hidden;
}

:global(.el-drawer.review-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 10px 18px;
  border-bottom: 1px solid var(--color-border);
}

:global(.el-drawer.review-drawer .el-drawer__body) {
  min-height: 0;
  padding: 0;
}

.review-drawer-header { display: flex; align-items: center; gap: 16px; min-width: 0; }
.back-queue { display: inline-flex; align-items: center; gap: 6px; padding: 0; color: #30463d; font-size: 13px; font-weight: 600; background: transparent; border: 0; cursor: pointer; }
.back-queue:hover { color: var(--color-primary); }
.back-queue span { font-size: 24px; line-height: 1; }
.review-drawer-header__path { color: var(--color-muted); font-size: 12px; }
.review-drawer-header__path span { padding: 0 6px; color: #b2bbb6; }

.case-queue { position: relative; display: flex; min-width: 0; flex-direction: column; background: #fff; border-right: 1px solid var(--color-border); overflow: hidden; }
.case-queue__toolbar { display: flex; justify-content: space-between; gap: 8px; padding: 12px 14px; color: var(--color-muted); font-size: 11px; border-bottom: 1px solid var(--color-border); }
.case-queue__list { overflow-y: auto; }
.case-item { display: grid; gap: 5px; width: 100%; padding: 12px 14px; color: var(--color-ink); text-align: left; background: #fff; border: 0; border-bottom: 1px solid #eef1ef; cursor: pointer; }
.case-item:hover { background: #f5f8f6; }
.case-item--active { padding-left: 11px; background: #edf6f1; border-left: 3px solid var(--color-primary); }
.case-item :deep(.el-tag) { width: max-content; }
.case-item strong { overflow: hidden; font-size: 13px; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.case-item span:not(.el-tag) { color: var(--color-muted); font-size: 11px; }
.case-item small { color: #87938d; font-size: 10px; }
.case-queue__count { margin-top: auto; padding: 12px 14px; color: var(--color-muted); font-size: 11px; border-top: 1px solid var(--color-border); }

.review-main {
  min-width: 0;
  padding: 24px;
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
}

.review-headline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 2px 20px;
  border-bottom: 1px solid var(--color-border);
}

.review-headline__copy { min-width: 0; }
.status-stack { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 5px; max-width: 180px; }

.scene-label {
  display: inline-flex;
  padding: 3px 7px;
  color: #176b4e;
  font-size: 11px;
  background: #e5f1ea;
  border-radius: 4px;
}

.review-headline h2 {
  margin: 8px 0 0;
  color: var(--color-ink);
  font-size: 23px;
  line-height: 1.35;
}

.review-headline p,
.section-heading p,
.decision-panel__heading p {
  margin: 5px 0 0;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.55;
}

.evidence-section,
.facts-section,
.rules-section {
  margin-top: 16px;
  padding: 18px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  animation: section-enter 0.28s ease both;
}

.facts-section { animation-delay: 0.04s; }
.rules-section { animation-delay: 0.08s; }

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-heading h3,
.summary-panel h3,
.decision-panel h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  color: var(--color-ink);
  font-size: 14px;
}

.evidence-count {
  flex: none;
  padding: 3px 7px;
  color: #6b746f;
  font-size: 11px;
  background: #f0f2ef;
  border-radius: 4px;
}

.description-panel {
  min-height: 94px;
  padding: 15px;
  color: #26322d;
  line-height: 1.75;
  white-space: pre-wrap;
  background: #fafaf8;
  border-left: 3px solid #2a825f;
}

.evidence-state {
  display: flex;
  align-items: center;
  gap: 9px;
  min-height: 70px;
  color: var(--color-muted);
  font-size: 12px;
}

.evidence-state--error { display: grid; justify-items: start; color: var(--color-danger); }
.evidence-state--error .el-button { margin-top: 2px; }
.evidence-tabs { margin-top: 14px; }
.evidence-tabs { max-width: 100%; overflow: hidden; }
.evidence-tabs :deep(.el-tabs__header) { margin-bottom: 10px; }
.evidence-tabs :deep(.el-tabs__nav-wrap),
.evidence-tabs :deep(.el-tabs__nav-scroll) { overflow-x: auto; }
.evidence-tabs :deep(.el-tabs__nav) { min-width: max-content; }
.evidence-tabs :deep(.el-tabs__item) { height: 34px; padding: 0 10px; color: var(--color-muted); font-size: 12px; }
.evidence-tabs :deep(.el-tabs__item.is-active) { color: var(--color-primary); font-weight: 650; }

.media-list,
.application-list,
.participant-list,
.timeline-list { display: grid; gap: 7px; }

.media-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 10px;
  color: inherit;
  text-align: left;
  background: #fafbf9;
  border: 1px solid #e3e9e4;
  border-radius: 5px;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.media-item:hover { background: #f1f7f3; border-color: #8cbaa1; }
.media-item:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.media-item > .el-icon:first-child { color: var(--color-primary); font-size: 18px; }
.media-item > .el-icon:last-child { color: var(--color-muted); }
.media-item > span { display: grid; min-width: 0; gap: 2px; }
.media-item strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.media-item small,
.application-item small,
.participant-copy small { color: var(--color-muted); font-size: 11px; }

.application-item { padding: 11px 12px; background: #fafbf9; border: 1px solid #e3e9e4; border-radius: 5px; }
.row-between { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.application-item strong,
.participant-copy strong { display: block; font-size: 12px; }
.application-item p { margin: 8px 0 0; color: #394740; font-size: 12px; line-height: 1.55; white-space: pre-wrap; }
.application-item ol { margin: 8px 0 0; padding-left: 19px; color: var(--color-muted); font-size: 11px; line-height: 1.55; }
.state-pill { display: inline-flex; align-items: center; width: max-content; min-height: 22px; padding: 2px 7px; color: #176b4e; font-size: 11px; background: #e8f3ed; border-radius: 4px; }

.participant-item { display: flex; align-items: center; gap: 9px; padding: 9px 10px; background: #fafbf9; border: 1px solid #e3e9e4; border-radius: 5px; }
.participant-avatar { display: grid; width: 28px; height: 28px; place-items: center; color: var(--color-primary); background: #e7f2eb; border-radius: 50%; }
.participant-copy { display: grid; flex: 1; min-width: 0; gap: 2px; }
.participant-copy strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.application-reason { color: #394740; font-size: 11px; line-height: 1.5; }

.timeline-item { display: grid; grid-template-columns: 12px minmax(0, 1fr); gap: 10px; padding: 3px 0 9px; }
.timeline-item > div { min-width: 0; }
.timeline-dot { position: relative; width: 9px; height: 9px; margin-top: 4px; background: #fff; border: 2px solid #a9b6ae; border-radius: 50%; }
.timeline-item:not(:last-child) .timeline-dot::after { position: absolute; top: 7px; left: 2px; width: 1px; height: calc(100% + 15px); background: #dce4df; content: ''; }
.timeline-item time { color: var(--color-muted); font-size: 10px; }
.timeline-item p { margin: 4px 0 0; color: var(--color-muted); font-size: 11px; line-height: 1.5; }
.timeline-item small { display: block; margin-top: 4px; color: #394740; font-size: 11px; line-height: 1.5; white-space: pre-wrap; }
.tab-empty { display: grid; min-height: 84px; place-items: center; color: var(--color-muted); font-size: 12px; background: #fafbf9; border: 1px dashed #d4ddd6; border-radius: 5px; }

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  padding: 1px;
  margin: 0;
  background: var(--color-border);
  border-radius: 5px;
  overflow: hidden;
}

.fact-item {
  min-width: 0;
  padding: 13px 14px;
  background: #fff;
}

.fact-item--wide { grid-column: 1 / -1; }

.fact-item dt,
.rules-list dt,
.summary-panel dt {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 5px;
  color: var(--color-muted);
  font-size: 11px;
}

.fact-item dd,
.rules-list dd,
.summary-panel dd {
  margin: 0;
  color: #26322d;
  font-size: 13px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.rules-list { margin: 0; }
.rules-list > div + div { margin-top: 14px; padding-top: 14px; border-top: 1px solid #edf0ed; }
.rules-list ol { margin: 0; padding-left: 19px; }
.rules-list li + li { margin-top: 6px; }

.review-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 20px;
  background: #fff;
  overflow-y: auto;
}

.summary-panel {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--color-border);
}

.summary-panel dl { margin: 14px 0 0; }
.summary-panel dl > div + div { margin-top: 11px; }

.decision-panel {
  position: sticky;
  top: 0;
  margin-top: 18px;
}

.decision-panel__heading {
  padding: 13px;
  background: #f2f6f3;
  border: 1px solid #dfe8e2;
  border-radius: 6px;
}

.decision-options {
  display: grid;
  gap: 6px;
  margin-top: 14px;
}

.decision-options :deep(.el-radio) {
  height: auto;
  min-height: 38px;
  padding: 8px 10px;
  margin-right: 0;
  border: 1px solid var(--color-border);
  border-radius: 5px;
}

.decision-options :deep(.approve-option.is-checked) { background: #edf7f1; border-color: #63a584; }
.decision-options :deep(.reject-option.is-checked) { background: #fff1ee; border-color: #d6857d; }
.decision-options :deep(.reject-option.is-checked .el-radio__label) { color: #a5352d; }

.decision-reason { margin-top: 12px; }

.review-actions {
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: auto 1fr;
  margin-top: auto;
  padding: 18px 0 0;
  background: #fff;
}

.review-actions .el-button { min-width: 0; margin: 0; }
.review-actions--readonly { grid-template-columns: 1fr; }

@keyframes section-enter {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 780px) {
  .review-workbench { display: block; height: auto; min-height: 0; overflow: visible; }
  .case-queue { max-height: 260px; border-right: 0; border-bottom: 1px solid var(--color-border); }
  .review-main { padding: 18px; border-right: 0; }
  .review-aside { padding: 18px; border-top: 1px solid var(--color-border); }
  .fact-grid { grid-template-columns: 1fr; }
  .fact-item--wide { grid-column: auto; }
  .decision-panel { position: static; }
  .review-actions { position: static; margin-top: 18px; }
}

@media (prefers-reduced-motion: reduce) {
  .evidence-section,
  .facts-section,
  .rules-section { animation: none; }
  .media-item { transition: none; }
}
</style>
