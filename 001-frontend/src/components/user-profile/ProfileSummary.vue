<script setup lang="ts">
import { computed } from 'vue'
import type { User } from '../../types'

const props = defineProps<{
  user: User | null
  roles: string[]
  completion: number
  completeCount: number
  totalCount: number
  missingFields: string[]
}>()

const roleLabelMap: Record<string, string> = {
  ADMIN: '系统管理员',
  STAFF: '物品保管员',
  USER: '普通用户'
}

const displayName = computed(() => props.user?.realName || props.user?.username || '未登录用户')
const username = computed(() => props.user?.username || '-')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const roleText = computed(() => props.roles.map((role) => roleLabelMap[role] || role).join('、') || '普通用户')
const statusText = computed(() => (props.user?.status === 'ENABLED' ? '账号正常' : '账号停用'))
const missingText = computed(() => {
  if (props.missingFields.length === 0) return '资料已完整，后续认领核验更顺畅。'
  return `还差 ${props.missingFields.join('、')}，补齐后便于后台联系和线下交接。`
})
</script>

<template>
  <section class="profile-summary">
    <div class="identity-block">
      <div class="avatar" aria-hidden="true">{{ avatarText }}</div>
      <div class="identity-copy">
        <span class="eyebrow">校园失物招领账户</span>
        <h1 class="profile-name">{{ displayName }}</h1>
        <p class="profile-meta">@{{ username }} · {{ roleText }}</p>
        <div class="identity-tags">
          <el-tag type="success" effect="light">{{ statusText }}</el-tag>
          <el-tag effect="plain">实名资料 {{ completeCount }}/{{ totalCount }}</el-tag>
        </div>
      </div>
    </div>

    <div class="completion-card">
      <div class="completion-head">
        <span>资料完整度</span>
        <strong>{{ completion }}%</strong>
      </div>
      <el-progress :percentage="completion" :show-text="false" :stroke-width="10" />
      <p class="completion-copy">{{ missingText }}</p>
    </div>
  </section>
</template>

<style scoped>
.profile-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 18px;
  padding: 22px;
  border: 1px solid #cfe1f3;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(27, 117, 187, 0.14), rgba(47, 158, 115, 0.1)),
    #fff;
}

.identity-block {
  display: flex;
  gap: 18px;
  align-items: center;
  min-width: 0;
}

.avatar {
  display: grid;
  flex: 0 0 74px;
  width: 74px;
  height: 74px;
  place-items: center;
  border: 3px solid #fff;
  border-radius: 22px;
  background: linear-gradient(135deg, var(--brand-deep), #2e9bc9);
  color: #fff;
  font-size: 30px;
  font-weight: 800;
  box-shadow: 0 14px 30px rgba(15, 79, 134, 0.22);
}

.identity-copy {
  min-width: 0;
}

.eyebrow {
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

.profile-name {
  margin: 6px 0;
  font-size: 32px;
  line-height: 1.15;
}

.profile-meta {
  margin: 0;
  color: #52677c;
}

.identity-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.completion-card {
  padding: 18px;
  border: 1px solid #d8e7f5;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
}

.completion-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #40566b;
  font-weight: 700;
}

.completion-head strong {
  color: var(--brand);
  font-size: 22px;
}

.completion-copy {
  margin: 14px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .profile-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .identity-block {
    align-items: flex-start;
  }

  .avatar {
    flex-basis: 58px;
    width: 58px;
    height: 58px;
    border-radius: 16px;
    font-size: 24px;
  }

  .profile-name {
    font-size: 25px;
  }
}
</style>
