<script setup lang="ts">
import { computed, onMounted, shallowRef } from 'vue'
import { storeToRefs } from 'pinia'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  FileDoneOutlined,
  IdcardOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { DashboardView } from '@/types/models'
import PageHeader from '@/components/common/PageHeader.vue'

const auth = useAuthStore()
const { user } = storeToRefs(auth)
const loading = shallowRef(true)
const stats = shallowRef<DashboardView>({
  activeChildren: 0,
  pendingRequests: 0,
  openRequests: 0,
  activeVolunteers: 0,
  activeAssignments: 0,
  completedAssignments: 0,
})

const statItems = computed(() => [
  { key: 'activeChildren', label: user.value?.roleCode === 'VOLUNTEER' ? '公开需求' : '有效儿童档案', value: user.value?.roleCode === 'VOLUNTEER' ? stats.value.openRequests : stats.value.activeChildren, icon: IdcardOutlined, color: '#2563eb' },
  { key: 'pendingRequests', label: '待审核需求', value: stats.value.pendingRequests, icon: ClockCircleOutlined, color: '#e67700' },
  { key: 'openRequests', label: '进行中需求', value: stats.value.openRequests, icon: FileDoneOutlined, color: '#087f5b' },
  { key: 'activeVolunteers', label: '认证志愿者', value: stats.value.activeVolunteers, icon: TeamOutlined, color: '#d9485f' },
  { key: 'activeAssignments', label: '进行中任务', value: stats.value.activeAssignments, icon: SafetyCertificateOutlined, color: '#0f7490' },
  { key: 'completedAssignments', label: '已完成任务', value: stats.value.completedAssignments, icon: CheckCircleOutlined, color: '#2f9e44' },
])

const quickLinks = computed(() => {
  switch (user.value?.roleCode) {
    case 'VOLUNTEER': return [
      { title: '查看需求大厅', text: '浏览当前可申请的脱敏帮扶需求', to: '/app/aid-hall' },
      { title: '维护认证资料', text: '完善服务区域、技能与可服务时间', to: '/app/profile' },
      { title: '查看服务任务', text: '记录回访并提交完成验收', to: '/app/assignments' },
    ]
    case 'CASE_WORKER': return [
      { title: '建立儿童档案', text: '维护本人负责的儿童基础资料', to: '/app/children' },
      { title: '发起帮扶需求', text: '从有效档案创建可审核需求', to: '/app/aid-requests' },
      { title: '跟踪服务任务', text: '查看回访、验收与评价状态', to: '/app/assignments' },
    ]
    case 'SUPERVISOR': return [
      { title: '审核儿童档案', text: '处理本部门待审核档案', to: '/app/children' },
      { title: '审核与匹配', text: '审核需求并选择合适志愿者', to: '/app/aid-requests' },
      { title: '服务验收', text: '核对回访记录并确认完成', to: '/app/assignments' },
    ]
    default: return [
      { title: '用户与部门', text: '创建内部账号并维护组织关系', to: '/app/admin/users' },
      { title: '志愿者审核', text: '处理待审核的志愿认证资料', to: '/app/admin/volunteers' },
      { title: '安全审计', text: '查看关键状态与账号操作记录', to: '/app/admin/audit' },
    ]
  }
})

onMounted(async () => {
  try {
    stats.value = await api.get<DashboardView>('/dashboard/summary')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-stack">
    <PageHeader :title="`你好，${user?.displayName ?? ''}`" subtitle="这里汇总与你当前职责相关的业务状态。" />
    <a-spin :spinning="loading">
      <section class="stats-grid" aria-label="业务统计">
        <article v-for="item in statItems" :key="item.key" class="stat-item surface">
          <span class="stat-icon" :style="{ color: item.color, backgroundColor: `${item.color}14` }">
            <component :is="item.icon" />
          </span>
          <div><strong>{{ item.value }}</strong><p>{{ item.label }}</p></div>
        </article>
      </section>
    </a-spin>
    <section class="quick-section">
      <h2>常用工作</h2>
      <div class="quick-grid">
        <RouterLink v-for="link in quickLinks" :key="link.to" class="quick-item surface" :to="link.to">
          <strong>{{ link.title }}</strong>
          <span>{{ link.text }}</span>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 104px;
  padding: 18px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  flex: 0 0 48px;
  font-size: 22px;
  border-radius: 6px;
}

.stat-item strong {
  font-size: 26px;
}

.stat-item p {
  margin: 3px 0 0;
  color: #68746e;
}

.quick-section {
  margin-top: 10px;
}

.quick-section h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.quick-item {
  display: grid;
  gap: 7px;
  min-height: 106px;
  padding: 20px;
  color: inherit;
  text-decoration: none;
}

.quick-item:hover {
  border-color: #85b7a5;
}

.quick-item strong {
  color: #163c31;
  font-size: 16px;
}

.quick-item span {
  color: #68746e;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .stats-grid,
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
