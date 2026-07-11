<script setup lang="ts">
import { computed, shallowRef, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import {
  AppstoreOutlined,
  AuditOutlined,
  CloseOutlined,
  FileDoneOutlined,
  FileSearchOutlined,
  HeartFilled,
  IdcardOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ProfileOutlined,
  SafetyCertificateOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { RoleCode } from '@/types/models'

interface NavItem {
  key: string
  label: string
  icon: Component
  roles?: RoleCode[]
  permission?: string
}

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const { user } = storeToRefs(auth)
const collapsed = shallowRef(false)

const allItems: NavItem[] = [
  { key: '/app/dashboard', label: '工作台', icon: AppstoreOutlined, permission: 'dashboard:read' },
  { key: '/app/aid-hall', label: '需求大厅', icon: FileSearchOutlined, roles: ['VOLUNTEER'] },
  { key: '/app/applications', label: '我的申请', icon: ProfileOutlined, roles: ['VOLUNTEER'], permission: 'application:read' },
  { key: '/app/children', label: '儿童档案', icon: IdcardOutlined, roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'child:read' },
  { key: '/app/aid-requests', label: '帮扶需求', icon: FileDoneOutlined, roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'aid:read' },
  { key: '/app/assignments', label: '服务任务', icon: SafetyCertificateOutlined, permission: 'assignment:read' },
  { key: '/app/admin/volunteers', label: '志愿者审核', icon: TeamOutlined, roles: ['SYS_ADMIN'], permission: 'volunteer:review' },
  { key: '/app/admin/users', label: '用户与部门', icon: UserOutlined, roles: ['SYS_ADMIN'], permission: 'system:user:manage' },
  { key: '/app/admin/roles', label: '角色权限', icon: SettingOutlined, roles: ['SYS_ADMIN'], permission: 'system:role:manage' },
  { key: '/app/admin/audit', label: '审计日志', icon: AuditOutlined, roles: ['SYS_ADMIN'], permission: 'system:audit:read' },
  { key: '/app/profile', label: '个人中心', icon: ProfileOutlined },
]

const menuItems = computed(() => allItems.filter((item) => {
  if (item.roles && (!user.value || !item.roles.includes(user.value.roleCode))) return false
  if (item.permission && !auth.hasPermission(item.permission)) return false
  return true
}))
const selectedKeys = computed(() => [String(route.meta.activeMenu ?? route.path)])
const roleLabel = computed(() => ({
  SYS_ADMIN: '系统管理员',
  SUPERVISOR: '部门主管',
  CASE_WORKER: '个案人员',
  VOLUNTEER: '志愿者',
}[user.value?.roleCode ?? 'VOLUNTEER']))

async function onAccountMenu({ key }: { key: string | number }) {
  if (key === 'profile') {
    await router.push('/app/profile')
  }
  if (key === 'logout') {
    await auth.logout()
    await router.replace('/')
  }
}

function onMenuClick({ key }: { key: string | number }) {
  router.push(String(key))
  if (window.matchMedia('(max-width: 991px)').matches) collapsed.value = true
}
</script>

<template>
  <a-layout class="workspace-layout">
    <button
      v-if="!collapsed"
      class="workspace-sider-backdrop"
      type="button"
      aria-label="关闭导航"
      @click="collapsed = true"
    />
    <a-layout-sider
      v-model:collapsed="collapsed"
      class="workspace-sider"
      :width="232"
      :collapsed-width="0"
      breakpoint="lg"
      :trigger="null"
      collapsible
    >
      <a-button class="mobile-sider-close" type="text" aria-label="关闭导航" @click="collapsed = true">
        <template #icon><CloseOutlined /></template>
      </a-button>
      <RouterLink class="workspace-brand" to="/">
        <span class="workspace-mark"><HeartFilled /></span>
        <span v-show="!collapsed">童伴关爱</span>
      </RouterLink>
      <a-menu :selected-keys="selectedKeys" theme="dark" mode="inline" @click="onMenuClick">
        <a-menu-item v-for="item in menuItems" :key="item.key">
          <template #icon><component :is="item.icon" /></template>
          {{ item.label }}
        </a-menu-item>
      </a-menu>
      </a-layout-sider>
    <a-layout>
      <a-layout-header class="workspace-header">
        <a-button class="collapse-button" type="text" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="collapsed = !collapsed">
          <template #icon>
            <MenuUnfoldOutlined v-if="collapsed" />
            <MenuFoldOutlined v-else />
          </template>
        </a-button>
        <span class="route-title">{{ route.meta.title }}</span>
        <a-dropdown :trigger="['click']">
          <button class="account-button" type="button">
            <a-avatar :size="34"><UserOutlined /></a-avatar>
            <span class="account-copy">
              <strong>{{ user?.displayName }}</strong>
              <small>{{ roleLabel }}</small>
            </span>
          </button>
          <template #overlay>
            <a-menu @click="onAccountMenu">
              <a-menu-item key="profile"><ProfileOutlined /> 个人中心</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout"><LogoutOutlined /> 退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-layout-header>
      <a-layout-content class="workspace-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.workspace-layout {
  min-height: 100vh;
}

.workspace-sider-backdrop,
.mobile-sider-close {
  display: none;
}

.workspace-sider {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: auto;
  background: #18302b !important;
}

.workspace-brand {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #ffffff;
  font-size: 19px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.workspace-mark {
  width: 32px;
  min-width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  color: #0b6b4f;
  background: #d9f4e8;
  border-radius: 5px;
}

.workspace-header {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  line-height: normal;
  background: #ffffff;
  border-bottom: 1px solid #dde5e1;
}

.collapse-button {
  width: 40px;
  height: 40px;
}

.route-title {
  color: #24322d;
  font-size: 16px;
  font-weight: 600;
}

.account-button {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-left: auto;
  padding: 4px 6px;
  color: inherit;
  background: transparent;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
}

.account-button:hover,
.account-button:focus-visible {
  background: #f1f5f3;
  outline: 2px solid transparent;
}

.account-copy {
  display: grid;
  text-align: left;
}

.account-copy strong {
  font-size: 14px;
}

.account-copy small {
  color: #738078;
  font-size: 12px;
}

.workspace-content {
  min-width: 0;
  padding: 22px;
  background: #f4f7f6;
}

@media (max-width: 768px) {
  .workspace-header {
    padding: 0 12px;
  }

  .workspace-content {
    padding: 14px;
  }

  .account-copy {
    display: none;
  }
}

@media (max-width: 991px) {
  .workspace-sider {
    position: fixed !important;
    inset: 0 auto 0 0;
    z-index: 1001;
    height: 100dvh;
    box-shadow: 8px 0 28px rgba(15, 42, 34, 0.2);
  }

  .workspace-sider-backdrop {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: block;
    padding: 0;
    background: rgba(16, 35, 29, 0.38);
    border: 0;
  }

  .mobile-sider-close {
    position: absolute;
    top: 18px;
    right: 12px;
    z-index: 1;
    display: inline-flex;
    width: 32px;
    height: 32px;
    color: #ffffff;
  }

  .mobile-sider-close:hover,
  .mobile-sider-close:focus-visible {
    color: #087f5b !important;
    background: #e8f4ef !important;
  }

  .workspace-brand {
    padding-right: 54px;
  }
}
</style>
