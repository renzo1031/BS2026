<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.user?.realName || '个人中心')

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <div class="public-layout">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">LF</span>
        <span>校园失物招领中心</span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/items">失物招领</RouterLink>
        <RouterLink v-if="auth.isAuthed" to="/user">个人中心</RouterLink>
        <RouterLink v-if="auth.isStaff" to="/admin">管理后台</RouterLink>
      </nav>
      <div class="account">
        <template v-if="auth.isAuthed">
          <span>{{ displayName }}</span>
          <el-button size="small" @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button size="small" @click="router.push('/login')">登录</el-button>
          <el-button size="small" type="primary" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>
    <main class="public-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.topbar {
  height: 64px;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 26px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.92);
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  color: var(--brand-deep);
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--brand), var(--green));
}

.nav {
  display: flex;
  gap: 18px;
  color: #344054;
  flex: 1;
}

.nav a.router-link-active {
  color: var(--brand);
  font-weight: 700;
}

.account {
  display: flex;
  gap: 10px;
  align-items: center;
}

.public-main {
  padding: 28px 0 42px;
}

@media (max-width: 760px) {
  .topbar {
    height: auto;
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 18px;
  }

  .nav {
    flex-wrap: wrap;
  }
}
</style>
