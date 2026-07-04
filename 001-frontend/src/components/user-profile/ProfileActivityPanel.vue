<script setup lang="ts">
interface ActivityItem {
  id: string
  title: string
  meta: string
  status: string
  time: string
  to: string
}

defineProps<{
  activities: ActivityItem[]
}>()
</script>

<template>
  <section class="profile-panel activity-panel">
    <div class="panel-head">
      <div>
        <h2 class="panel-title">最近动态</h2>
        <p class="panel-copy">汇总你的发布、认领和线索记录，方便继续处理。</p>
      </div>
      <RouterLink to="/user/items" class="panel-link">查看全部</RouterLink>
    </div>

    <el-empty v-if="activities.length === 0" description="暂无个人业务动态" />
    <div v-else class="activity-list">
      <RouterLink v-for="activity in activities" :key="activity.id" :to="activity.to" class="activity-row">
        <span class="activity-dot" aria-hidden="true"></span>
        <span class="activity-main">
          <strong>{{ activity.title }}</strong>
          <small>{{ activity.meta }}</small>
        </span>
        <span class="activity-side">
          <el-tag size="small" effect="plain">{{ activity.status }}</el-tag>
          <small>{{ activity.time }}</small>
        </span>
      </RouterLink>
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

.activity-list {
  display: grid;
  gap: 10px;
}

.activity-row {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #e2edf7;
  border-radius: 8px;
  background: #fbfdff;
}

.activity-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--brand);
  box-shadow: 0 0 0 4px rgba(27, 117, 187, 0.12);
}

.activity-main,
.activity-side {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.activity-main strong,
.activity-main small,
.activity-side small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-main small,
.activity-side small {
  color: var(--muted);
  font-size: 12px;
}

.activity-side {
  justify-items: end;
}

@media (max-width: 720px) {
  .panel-head,
  .activity-row {
    grid-template-columns: 1fr;
  }

  .panel-head {
    display: grid;
  }

  .activity-dot {
    display: none;
  }

  .activity-side {
    justify-items: start;
  }
}
</style>
