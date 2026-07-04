<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const collapsed = shallowRef(false)
const isMobile = shallowRef(false)
const mobileOpen = shallowRef(false)
const mobileMediaQuery = '(max-width: 900px)'

const menus = computed(() => {
  const all = [
    { path: '/admin', label: '后台首页', roles: ['ADMIN', 'STAFF'] },
    { path: '/admin/item-review', label: '物品审核', roles: ['ADMIN'] },
    { path: '/admin/items', label: '全量物品', roles: ['ADMIN'] },
    { path: '/admin/claims', label: '认领核验', roles: ['ADMIN', 'STAFF'] },
    { path: '/admin/clues', label: '线索管理', roles: ['ADMIN'] },
    { path: '/admin/handover', label: '保管交接', roles: ['ADMIN', 'STAFF'] },
    { path: '/admin/users', label: '用户管理', roles: ['ADMIN'] },
    { path: '/admin/taxonomy', label: '分类地点', roles: ['ADMIN'] },
    { path: '/admin/notices', label: '公告管理', roles: ['ADMIN'] },
    { path: '/admin/logs', label: '操作日志', roles: ['ADMIN'] }
  ]
  return all.filter((menu) => auth.hasAnyRole(menu.roles))
})

const layoutClasses = computed(() => ({
  'is-collapsed': collapsed.value,
  'is-mobile': isMobile.value,
  'is-mobile-open': mobileOpen.value
}))

const toggleLabel = computed(() => {
  if (isMobile.value) {
    return mobileOpen.value ? '关闭菜单' : '打开菜单'
  }
  return collapsed.value ? '展开' : '收起'
})

const sidebarExpanded = computed(() => {
  if (isMobile.value) {
    return mobileOpen.value
  }
  return !collapsed.value
})

function syncViewport() {
  isMobile.value = window.matchMedia(mobileMediaQuery).matches
  if (!isMobile.value) {
    mobileOpen.value = false
  }
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileOpen.value = !mobileOpen.value
    return
  }
  collapsed.value = !collapsed.value
}

function closeMobileSidebar() {
  mobileOpen.value = false
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeMobileSidebar()
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="admin-layout" :class="layoutClasses">
    <div v-if="mobileOpen" class="admin-mask" @click="closeMobileSidebar"></div>
    <aside id="admin-sidebar" class="admin-side" :aria-hidden="isMobile && !mobileOpen">
      <RouterLink class="admin-brand" to="/" @click="closeMobileSidebar">
        <span class="brand-mark">LF</span>
        <span class="brand-text">校园失物招领</span>
      </RouterLink>
      <RouterLink v-for="menu in menus" :key="menu.path" :to="menu.path" @click="closeMobileSidebar">
        <span class="menu-dot">{{ menu.label.slice(0, 1) }}</span>
        <span class="menu-text">{{ menu.label }}</span>
      </RouterLink>
    </aside>
    <section class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-button
            size="small"
            aria-controls="admin-sidebar"
            :aria-expanded="sidebarExpanded"
            @click="toggleSidebar"
          >
            {{ toggleLabel }}
          </el-button>
          <span>{{ auth.user?.realName || '管理后台' }}</span>
        </div>
        <div class="header-actions">
          <el-button size="small" @click="router.push('/')">门户</el-button>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </header>
      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 238px minmax(0, 1fr);
  background: #eef4fa;
  transition: grid-template-columns 0.18s ease;
}

.admin-layout.is-collapsed {
  grid-template-columns: 76px minmax(0, 1fr);
}

.admin-side {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  background: #123b5d;
  color: #dcecff;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-brand {
  color: #fff;
  font-weight: 800;
  font-size: 18px;
  padding: 12px 10px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark,
.menu-dot {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  color: #fff;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 13px;
}

.menu-dot {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 13px;
  font-weight: 700;
}

.admin-side a {
  padding: 11px 12px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-side a.router-link-exact-active {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
}

.admin-layout.is-collapsed .admin-side {
  padding: 14px 10px;
}

.admin-layout.is-collapsed .admin-brand,
.admin-layout.is-collapsed .admin-side a {
  justify-content: center;
  padding-left: 8px;
  padding-right: 8px;
}

.admin-layout.is-collapsed .brand-text,
.admin-layout.is-collapsed .menu-text {
  display: none;
}

.admin-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: #fff;
  border-bottom: 1px solid var(--line);
}

.admin-main {
  min-width: 0;
}

.header-left {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.admin-content {
  min-width: 0;
  padding: 20px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-side {
    position: fixed;
    z-index: 30;
    left: 0;
    top: 0;
    width: min(280px, 82vw);
    transform: translateX(-100%);
    transition: transform 0.18s ease;
    box-shadow: 14px 0 32px rgba(15, 35, 55, 0.24);
  }

  .admin-layout.is-mobile-open .admin-side {
    transform: translateX(0);
  }

  .admin-layout.is-collapsed {
    grid-template-columns: 1fr;
  }

  .admin-layout.is-collapsed .admin-side {
    padding: 18px;
  }

  .admin-layout.is-collapsed .admin-brand,
  .admin-layout.is-collapsed .admin-side a {
    justify-content: flex-start;
    padding: 11px 12px;
  }

  .admin-layout.is-collapsed .brand-text,
  .admin-layout.is-collapsed .menu-text {
    display: inline;
  }

  .admin-mask {
    position: fixed;
    inset: 0;
    z-index: 20;
    background: rgba(15, 35, 55, 0.36);
  }

  .admin-header {
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .admin-content {
    padding: 14px;
  }
}
</style>
