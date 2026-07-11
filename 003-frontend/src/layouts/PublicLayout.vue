<script setup lang="ts">
import { computed, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { HeartFilled, MenuOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PublicFooter from '@/components/layout/PublicFooter.vue'

const route = useRoute()
const auth = useAuthStore()
const drawerOpen = shallowRef(false)
const platformLink = computed(() => auth.isAuthenticated ? '/app/dashboard' : '/login')

const navItems = [
  { label: '首页', to: '/' },
  { label: '服务内容', to: '/#services' },
  { label: '工作流程', to: '/#process' },
  { label: '帮扶需求', to: '/aid-hall' },
]
</script>

<template>
  <div class="public-layout">
    <header class="public-header">
      <div class="container header-inner">
        <RouterLink class="brand" to="/" aria-label="童伴关爱首页">
          <span class="brand-mark"><HeartFilled /></span>
          <span>童伴关爱</span>
        </RouterLink>
        <nav class="desktop-nav" aria-label="主导航">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            :class="{ active: route.path === item.to }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>
        <div class="header-actions">
          <a-button :href="platformLink" type="primary">
            <template #icon><UserOutlined /></template>
            {{ auth.isAuthenticated ? '进入工作台' : '登录平台' }}
          </a-button>
          <a-tooltip title="打开导航">
            <a-button class="mobile-menu" type="text" aria-label="打开导航" @click="drawerOpen = true">
              <template #icon><MenuOutlined /></template>
            </a-button>
          </a-tooltip>
        </div>
      </div>
    </header>
    <main>
      <RouterView />
    </main>
    <PublicFooter />
    <a-drawer v-model:open="drawerOpen" title="导航" placement="right" :width="280">
      <nav class="mobile-nav" aria-label="移动端导航">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" @click="drawerOpen = false">
          {{ item.label }}
        </RouterLink>
      </nav>
    </a-drawer>
  </div>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  background: #ffffff;
}

.public-header {
  position: sticky;
  top: 0;
  z-index: 30;
  height: 68px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e4e9e7;
}

.header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #123b31;
  font-size: 20px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: #087f5b;
  border-radius: 6px;
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 30px;
  margin-left: auto;
}

.desktop-nav a {
  position: relative;
  color: #42504b;
  font-weight: 500;
  text-decoration: none;
}

.desktop-nav a:hover,
.desktop-nav a.active {
  color: #087f5b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-menu {
  display: none;
}

.mobile-nav {
  display: grid;
  gap: 8px;
}

.mobile-nav a {
  min-height: 44px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  color: #26332f;
  text-decoration: none;
  border-bottom: 1px solid #edf0ef;
}

@media (max-width: 820px) {
  .desktop-nav {
    display: none;
  }

  .mobile-menu {
    display: inline-flex;
  }

  .header-actions :deep(.ant-btn-primary span:not(.anticon)) {
    display: none;
  }
}
</style>
