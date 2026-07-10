<script setup>
import { computed, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, useMessage } from 'naive-ui'
import {
  CloseOutline,
  LogOutOutline,
  MenuOutline,
  PersonOutline,
  SchoolOutline
} from '@vicons/ionicons5'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const mobileMenuOpen = shallowRef(false)
const logoutLoading = shallowRef(false)

const navItems = [
  { label: '首页', path: '/' },
  { label: '办事大厅', path: '/apply' },
  { label: '我的申请', path: '/my-requests' },
  { label: '消息通知', path: '/notices' }
]

const displayName = computed(() => auth.user?.realName || auth.user?.username || '同学')
const avatarText = computed(() => displayName.value.slice(0, 1))

watch(() => route.fullPath, () => {
  mobileMenuOpen.value = false
})

function isActive(path) {
  if (path === '/') return route.path === '/'
  if (path === '/my-requests') return route.path === '/my-requests' || route.path.startsWith('/requests/')
  return route.path.startsWith(path)
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

<template>
  <a class="skip-link" href="#student-main">跳到主要内容</a>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="portal-header-inner">
        <router-link class="portal-brand" to="/" aria-label="大学生一体化服务平台首页">
          <span class="portal-brand-mark"><n-icon :component="SchoolOutline" /></span>
          <span class="portal-brand-copy">
            <strong>大学生一体化服务平台</strong>
            <small>Campus Service Center</small>
          </span>
        </router-link>

        <nav class="portal-nav" aria-label="学生门户导航">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            :class="['portal-nav-link', { active: isActive(item.path) }]"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="portal-user-actions">
          <router-link class="portal-user" to="/profile" aria-label="打开个人中心">
            <span class="portal-avatar">{{ avatarText }}</span>
            <span class="portal-user-name">{{ displayName }}</span>
          </router-link>
          <button
            class="portal-icon-button desktop-action"
            type="button"
            title="退出登录"
            aria-label="退出登录"
            :disabled="logoutLoading"
            @click="logout"
          >
            <n-icon :component="LogOutOutline" />
          </button>
          <button
            class="portal-icon-button mobile-menu-button"
            type="button"
            :aria-expanded="mobileMenuOpen"
            aria-controls="mobile-portal-nav"
            :aria-label="mobileMenuOpen ? '关闭导航' : '打开导航'"
            @click="mobileMenuOpen = !mobileMenuOpen"
          >
            <n-icon :component="mobileMenuOpen ? CloseOutline : MenuOutline" />
          </button>
        </div>
      </div>

      <nav id="mobile-portal-nav" v-show="mobileMenuOpen" class="mobile-portal-nav" aria-label="移动端学生门户导航">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="['mobile-nav-link', { active: isActive(item.path) }]"
        >
          {{ item.label }}
        </router-link>
        <router-link class="mobile-nav-link" to="/profile">
          <n-icon :component="PersonOutline" />
          个人中心
        </router-link>
        <button class="mobile-logout" type="button" :disabled="logoutLoading" @click="logout">
          <n-icon :component="LogOutOutline" />
          退出登录
        </button>
      </nav>
    </header>

    <main id="student-main" class="portal-main" tabindex="-1">
      <router-view />
    </main>

    <footer class="portal-footer">
      <div class="portal-footer-inner">
        <span>大学生一体化服务平台</span>
        <span>学生事务统一办理入口</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.portal-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f6f5;
  color: #23312e;
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid #dfe5e2;
  backdrop-filter: blur(12px);
}

.portal-header-inner,
.portal-footer-inner {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
}

.portal-header-inner {
  height: 74px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.portal-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  flex: 0 0 auto;
}

.portal-brand-mark {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  color: #fff;
  background: #205c50;
  border-radius: 4px;
  font-size: 24px;
}

.portal-brand-copy {
  display: grid;
  gap: 2px;
  line-height: 1.1;
}

.portal-brand-copy strong {
  font-family: "Songti SC", STSong, "Microsoft YaHei", serif;
  font-size: 17px;
  color: #173f37;
}

.portal-brand-copy small {
  font-size: 10px;
  color: #77847f;
  letter-spacing: 0;
}

.portal-nav {
  height: 100%;
  display: flex;
  align-items: stretch;
  margin-left: auto;
}

.portal-nav-link {
  position: relative;
  min-width: 88px;
  display: grid;
  place-items: center;
  padding: 0 16px;
  font-size: 15px;
  color: #4d5b57;
}

.portal-nav-link::after {
  content: "";
  position: absolute;
  right: 18px;
  bottom: 0;
  left: 18px;
  height: 3px;
  background: transparent;
}

.portal-nav-link:hover,
.portal-nav-link.active {
  color: #173f37;
}

.portal-nav-link.active::after {
  background: #a5413b;
}

.portal-user-actions,
.portal-user {
  display: flex;
  align-items: center;
}

.portal-user-actions {
  gap: 8px;
}

.portal-user {
  gap: 8px;
  padding-left: 14px;
  border-left: 1px solid #dfe5e2;
  color: #35433f;
}

.portal-avatar {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #a5413b;
  font-weight: 700;
}

.portal-user-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.portal-icon-button,
.mobile-logout {
  border: 0;
  color: #4d5b57;
  background: transparent;
  cursor: pointer;
}

.portal-icon-button {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 4px;
  font-size: 19px;
}

.portal-icon-button:hover,
.portal-icon-button:focus-visible {
  color: #173f37;
  background: #edf2f0;
}

.portal-icon-button:disabled,
.mobile-logout:disabled {
  cursor: wait;
  opacity: 0.5;
}

.mobile-menu-button,
.mobile-portal-nav {
  display: none;
}

.portal-main {
  flex: 1;
  min-width: 0;
}

.portal-footer {
  color: #dbe5e1;
  background: #173f37;
}

.portal-footer-inner {
  min-height: 78px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  font-size: 13px;
}

@media (max-width: 960px) {
  .portal-nav,
  .desktop-action {
    display: none;
  }

  .portal-user-actions {
    margin-left: auto;
  }

  .mobile-menu-button {
    display: grid;
  }

  .mobile-portal-nav {
    position: absolute;
    top: 100%;
    right: 0;
    left: 0;
    display: block;
    width: 100%;
    margin: 0;
    padding: 8px 20px 16px;
    background: #fff;
    border-top: 1px solid #edf0ef;
    border-bottom: 1px solid #dfe5e2;
    box-shadow: 0 14px 28px rgb(25 45 40 / 12%);
  }

  .mobile-nav-link,
  .mobile-logout {
    min-height: 44px;
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    padding: 0 12px;
    border-radius: 4px;
    font-size: 15px;
  }

  .mobile-nav-link.active {
    color: #173f37;
    background: #edf2f0;
    font-weight: 700;
  }
}

@media (max-width: 600px) {
  .portal-header-inner,
  .portal-footer-inner {
    width: min(100% - 28px, 1240px);
  }

  .portal-header-inner {
    height: 64px;
    gap: 12px;
  }

  .portal-brand-mark {
    width: 36px;
    height: 36px;
  }

  .portal-brand-copy strong {
    font-size: 15px;
  }

  .portal-brand-copy small,
  .portal-user-name {
    display: none;
  }

  .portal-user {
    padding-left: 0;
    border-left: 0;
  }

  .portal-footer-inner {
    min-height: 92px;
    align-items: flex-start;
    justify-content: center;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
