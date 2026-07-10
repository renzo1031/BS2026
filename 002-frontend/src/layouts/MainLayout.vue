<template>
  <a class="skip-link" href="#main-content">跳到主要内容</a>
  <n-layout has-sider class="shell">
    <n-layout-sider v-if="!isMobile" bordered :width="236">
      <div class="brand">学生一体化服务</div>
      <nav aria-label="主导航">
        <n-menu :value="route.path" :options="menuOptions" @update:value="go" />
      </nav>
    </n-layout-sider>

    <n-drawer v-model:show="drawerVisible" placement="left" :width="280">
      <n-drawer-content title="学生一体化服务" closable>
        <nav aria-label="移动端主导航">
          <n-menu :value="route.path" :options="menuOptions" @update:value="go" />
        </nav>
      </n-drawer-content>
    </n-drawer>

    <n-layout>
      <n-layout-header bordered class="header">
        <div class="header-user">
          <n-button v-if="isMobile" quaternary circle aria-label="打开导航" @click="drawerVisible = true">
            <template #icon><n-icon><MenuOutline /></n-icon></template>
          </n-button>
          <div class="identity">
            <strong>{{ auth.user?.realName || auth.user?.username }}</strong>
            <span class="muted">{{ roleText }}</span>
          </div>
        </div>
        <n-space :size="4">
          <n-button quaternary :aria-label="isMobile ? '消息通知' : undefined" @click="router.push('/notices')">
            <template #icon><n-icon><NotificationsOutline /></n-icon></template>
            <span v-if="!isMobile">消息</span>
          </n-button>
          <n-button quaternary :loading="logoutLoading" :aria-label="isMobile ? '退出登录' : undefined" @click="logout">
            <template #icon><n-icon><LogOutOutline /></n-icon></template>
            <span v-if="!isMobile">退出</span>
          </n-button>
        </n-space>
      </n-layout-header>
      <n-layout-content id="main-content" tabindex="-1">
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup>
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton, NDrawer, NDrawerContent, NIcon, NLayout, NLayoutContent,
  NLayoutHeader, NLayoutSider, NMenu, NSpace, useMessage
} from 'naive-ui'
import {
  AlbumsOutline,
  ClipboardOutline,
  CreateOutline,
  FolderOpenOutline,
  HomeOutline,
  ListOutline,
  LogOutOutline,
  MenuOutline,
  NotificationsOutline,
  PeopleOutline,
  PersonOutline,
  StatsChartOutline
} from '@vicons/ionicons5'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const drawerVisible = ref(false)
const logoutLoading = ref(false)
const isMobile = ref(false)
let mediaQuery

const icon = (component) => () => h(NIcon, null, { default: () => h(component) })

const menuOptions = computed(() => {
  const options = []
  if (auth.isStudent) {
    options.push(
      { label: '服务大厅', key: '/', icon: icon(HomeOutline) },
      { label: '提交申请', key: '/apply', icon: icon(CreateOutline) },
      { label: '我的申请', key: '/my-requests', icon: icon(ListOutline) }
    )
  }
  if (auth.isStaff || auth.isAdmin) {
    options.push(
      { label: '处理概览', key: '/admin/stats', icon: icon(StatsChartOutline) },
      { label: '申请处理', key: '/admin/requests', icon: icon(FolderOpenOutline) }
    )
  }
  if (auth.isAdmin) {
    options.push(
      { label: '用户管理', key: '/admin/users', icon: icon(PeopleOutline) },
      { label: '事项与场地', key: '/admin/catalog', icon: icon(AlbumsOutline) },
      { label: '操作日志', key: '/admin/logs', icon: icon(ClipboardOutline) }
    )
  }
  options.push(
    { label: '个人中心', key: '/profile', icon: icon(PersonOutline) },
    { label: '消息通知', key: '/notices', icon: icon(NotificationsOutline) }
  )
  return options
})

const roleText = computed(() => {
  if (auth.isAdmin) return '系统管理员'
  if (auth.isStaff) return '部门处理人员'
  return '学生'
})

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 768px)')
  updateViewport(mediaQuery)
  mediaQuery.addEventListener('change', updateViewport)
})

onBeforeUnmount(() => mediaQuery?.removeEventListener('change', updateViewport))

function updateViewport(event) {
  isMobile.value = event.matches
  if (!event.matches) drawerVisible.value = false
}

function go(path) {
  drawerVisible.value = false
  router.push(path)
}

async function logout() {
  logoutLoading.value = true
  try {
    await auth.logout()
  } catch (error) {
    message.warning(error.message || '服务端退出失败，本地会话已清理')
  } finally {
    logoutLoading.value = false
    router.replace('/login')
  }
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
}

.brand {
  height: 58px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 18px;
  font-weight: 800;
  color: #0f766e;
}

.header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 20px;
  background: #fff;
}

.header-user,
.identity {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.identity strong,
.identity span {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .header {
    padding: 0 10px;
  }

  .identity {
    display: grid;
    gap: 0;
    line-height: 1.25;
  }

  .identity strong,
  .identity span {
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
</style>
