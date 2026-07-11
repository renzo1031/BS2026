<script setup lang="ts">
import type { PublicOverview } from '@/types/models'

defineProps<{
  overview: PublicOverview
  loading: boolean
}>()

const items: Array<{ key: keyof PublicOverview; label: string; suffix: string }> = [
  { key: 'completedServices', label: '已完成服务', suffix: '项' },
  { key: 'approvedVolunteers', label: '认证志愿者', suffix: '名' },
  { key: 'activeRequests', label: '进行中需求', suffix: '项' },
  { key: 'serviceDepartments', label: '服务机构', suffix: '个' },
]
</script>

<template>
  <section class="impact-band">
    <div class="container impact-inner">
      <div>
        <p class="impact-label">公开数据</p>
        <h2>以真实业务记录汇总，不展示个人信息</h2>
      </div>
      <div class="impact-grid">
        <div v-for="item in items" :key="item.key" class="impact-item">
          <a-skeleton v-if="loading" :paragraph="false" active />
          <template v-else>
            <strong>{{ overview[item.key] }}</strong>
            <span>{{ item.suffix }}</span>
            <p>{{ item.label }}</p>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.impact-band {
  padding: 72px 0;
  background: #f1f6f4;
}

.impact-inner {
  display: grid;
  grid-template-columns: 1fr 1.7fr;
  gap: 60px;
  align-items: end;
}

.impact-label {
  margin: 0 0 10px;
  color: #087f5b;
  font-weight: 700;
}

.impact-inner h2 {
  max-width: 450px;
  margin: 0;
  font-size: 30px;
  line-height: 1.5;
}

.impact-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid #cbd8d3;
}

.impact-item {
  min-width: 0;
  padding: 26px 14px 0;
  border-right: 1px solid #cbd8d3;
}

.impact-item:last-child {
  border-right: 0;
}

.impact-item strong {
  color: #17352e;
  font-size: 34px;
}

.impact-item span {
  margin-left: 4px;
  color: #52635d;
}

.impact-item p {
  margin: 8px 0 0;
  color: #65736e;
}

@media (max-width: 880px) {
  .impact-inner {
    grid-template-columns: 1fr;
    gap: 38px;
  }
}

@media (max-width: 600px) {
  .impact-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .impact-item:nth-child(2) {
    border-right: 0;
  }

  .impact-item:nth-child(-n + 2) {
    padding-bottom: 22px;
    border-bottom: 1px solid #cbd8d3;
  }
}
</style>
