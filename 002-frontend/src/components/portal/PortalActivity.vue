<script setup>
import { NEmpty, NIcon, NSpin } from 'naive-ui'
import { ArrowForwardOutline } from '@vicons/ionicons5'
import { formatDateTime, statusText } from '../../utils/display'

defineProps({
  requests: { type: Array, default: () => [] },
  notices: { type: Array, default: () => [] },
  loading: Boolean,
  errorMessage: { type: String, default: '' }
})

const emit = defineEmits(['open-request', 'open-requests', 'open-notices', 'start'])

function statusClass(status) {
  if (['FINISHED', 'EVALUATED'].includes(status)) return 'done'
  if (['REJECTED', 'CANCELLED'].includes(status)) return 'closed'
  return 'active'
}
</script>

<template>
  <section class="activity-band" aria-labelledby="activity-title">
    <div class="activity-inner">
      <header class="activity-heading">
        <h2 id="activity-title">我的校园事务</h2>
        <p>最近申请与消息集中在这里，不用重复查找。</p>
      </header>

      <div v-if="loading" class="activity-loading"><n-spin size="large" /></div>
      <div v-else-if="errorMessage" class="activity-error">
        <strong>个人动态暂时无法加载</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <div v-else class="activity-grid">
        <section class="activity-column" aria-labelledby="recent-requests-title">
          <div class="column-title">
            <h3 id="recent-requests-title">最近申请</h3>
            <button type="button" @click="emit('open-requests')">
              查看全部 <n-icon :component="ArrowForwardOutline" />
            </button>
          </div>
          <div v-if="requests.length" class="activity-list">
            <button
              v-for="request in requests"
              :key="request.id"
              class="request-entry"
              type="button"
              @click="emit('open-request', request.id)"
            >
              <span class="request-main">
                <strong>{{ request.title }}</strong>
                <small>{{ request.requestNo }} · {{ formatDateTime(request.createdAt) }}</small>
              </span>
              <span :class="['request-status', statusClass(request.status)]">{{ statusText(request.status) }}</span>
            </button>
          </div>
          <div v-else class="activity-empty">
            <n-empty description="还没有申请记录" />
            <button class="empty-start" type="button" @click="emit('start')">办理第一项业务</button>
          </div>
        </section>

        <section class="activity-column notice-column" aria-labelledby="recent-notices-title">
          <div class="column-title">
            <h3 id="recent-notices-title">最新通知</h3>
            <button type="button" @click="emit('open-notices')">
              查看全部 <n-icon :component="ArrowForwardOutline" />
            </button>
          </div>
          <div v-if="notices.length" class="notice-list">
            <article v-for="notice in notices" :key="notice.id" class="notice-entry">
              <span :class="['notice-mark', { read: notice.readFlag === 1 || notice.readFlag === true }]" aria-hidden="true"></span>
              <div>
                <h4>{{ notice.title }}</h4>
                <p>{{ notice.content }}</p>
                <small>{{ formatDateTime(notice.createdAt) }}</small>
              </div>
            </article>
          </div>
          <div v-else class="activity-empty">
            <n-empty description="暂无新通知" />
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
.activity-band {
  background: #f4f6f5;
}

.activity-inner {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 64px 0 72px;
}

.activity-heading h2 {
  margin: 0;
  font-family: "Songti SC", STSong, "Microsoft YaHei", serif;
  font-size: 30px;
  color: #173f37;
}

.activity-heading p {
  margin: 10px 0 0;
  color: #6c7874;
}

.activity-loading {
  min-height: 280px;
  display: grid;
  place-items: center;
}

.activity-error {
  display: grid;
  gap: 6px;
  margin-top: 28px;
  padding: 20px;
  border-left: 4px solid #a5413b;
  background: #fff;
}

.activity-error span {
  color: #6c7874;
}

.activity-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 54px;
  margin-top: 32px;
}

.activity-column {
  min-width: 0;
}

.notice-column {
  padding-left: 42px;
  border-left: 1px solid #d9e0dd;
}

.column-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 13px;
  border-bottom: 2px solid #284f47;
}

.column-title h3 {
  margin: 0;
  font-size: 18px;
}

.column-title button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 0;
  color: #205c50;
  background: transparent;
  cursor: pointer;
}

.request-entry {
  width: 100%;
  min-height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 4px;
  border: 0;
  border-bottom: 1px solid #dfe5e2;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.request-entry:hover .request-main strong,
.request-entry:focus-visible .request-main strong {
  color: #205c50;
}

.request-main {
  min-width: 0;
  display: grid;
  gap: 8px;
}

.request-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-main small,
.notice-entry small {
  color: #87918e;
}

.request-status {
  flex: 0 0 auto;
  padding-left: 12px;
  border-left: 3px solid;
  font-size: 13px;
  font-weight: 700;
}

.request-status.active {
  color: #8b5b19;
  border-color: #c68c2f;
}

.request-status.done {
  color: #205c50;
  border-color: #205c50;
}

.request-status.closed {
  color: #8d4a46;
  border-color: #a5413b;
}

.notice-entry {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr);
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #dfe5e2;
}

.notice-mark {
  width: 7px;
  height: 7px;
  margin-top: 7px;
  border-radius: 50%;
  background: #a5413b;
}

.notice-mark.read {
  background: #aeb7b4;
}

.notice-entry h4 {
  margin: 0;
  font-size: 15px;
}

.notice-entry p {
  display: -webkit-box;
  margin: 7px 0;
  overflow: hidden;
  color: #65716d;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.activity-empty {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
}

.empty-start {
  min-height: 40px;
  padding: 0 17px;
  border: 0;
  border-radius: 4px;
  color: #fff;
  background: #205c50;
  font-weight: 700;
  cursor: pointer;
}

.empty-start:hover,
.empty-start:focus-visible {
  background: #173f37;
}

@media (max-width: 820px) {
  .activity-inner {
    width: calc(100% - 28px);
    padding: 50px 0 58px;
  }

  .activity-grid {
    grid-template-columns: 1fr;
    gap: 44px;
  }

  .notice-column {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
