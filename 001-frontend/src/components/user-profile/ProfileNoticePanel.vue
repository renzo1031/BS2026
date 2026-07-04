<script setup lang="ts">
import { computed } from 'vue'
import type { Notice } from '../../types'

const props = defineProps<{
  notices: Notice[]
}>()

const visibleNotices = computed(() => props.notices.slice(0, 3))

function formatDate(value?: string) {
  if (!value) return '未设置发布时间'
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <section class="profile-panel notice-panel">
    <div class="panel-head">
      <div>
        <h2 class="panel-title">校园公告</h2>
        <p class="panel-copy">与你当前业务流程相关的最新通知。</p>
      </div>
      <RouterLink to="/user/notices" class="panel-link">全部通知</RouterLink>
    </div>

    <el-empty v-if="visibleNotices.length === 0" description="暂无公告" />
    <div v-else class="notice-list">
      <article v-for="notice in visibleNotices" :key="notice.id" class="notice-row">
        <strong>{{ notice.title }}</strong>
        <p>{{ notice.content }}</p>
        <span>{{ formatDate(notice.publishedAt || notice.startTime) }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.profile-panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-title {
  margin: 0;
  font-size: 20px;
}

.panel-copy {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.panel-link {
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.notice-list {
  display: grid;
  gap: 12px;
}

.notice-row {
  padding-bottom: 12px;
  border-bottom: 1px solid #e8f0f8;
}

.notice-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.notice-row strong {
  display: block;
  color: #132f4b;
}

.notice-row p {
  display: -webkit-box;
  overflow: hidden;
  margin: 7px 0;
  color: #52677c;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notice-row span {
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 720px) {
  .panel-head {
    flex-direction: column;
  }
}
</style>
