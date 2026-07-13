<script setup lang="ts">
import { computed, shallowRef, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Calendar,
  CollectionTag,
  DataAnalysis,
  DocumentChecked,
  Files,
  House,
  Menu,
  School,
  Stamp,
  Tickets,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/api'

interface MenuItem {
  path: string
  label: string
  eyebrow: string
  icon: Component
  roles?: UserRole[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mobileMenuOpen = shallowRef(false)

const menuItems: MenuItem[] = [
  { path: '/', label: '工作台', eyebrow: '01', icon: House },
  { path: '/operations/activities', label: '活动列表', eyebrow: '02', icon: Calendar },
  { path: '/reviews/activities', label: '活动审核', eyebrow: '03', icon: DocumentChecked },
  { path: '/reviews/identities', label: '身份审核', eyebrow: '04', icon: Stamp },
  { path: '/governance/reports', label: '举报与申诉', eyebrow: '05', icon: Warning },
  { path: '/reviews/files', label: '文件审核', eyebrow: '06', icon: Files },
  { path: '/platform/users', label: '用户与审核员', eyebrow: '07', icon: UserFilled, roles: ['PLATFORM_ADMIN'] },
  { path: '/platform/campuses', label: '校园管理', eyebrow: '08', icon: School, roles: ['PLATFORM_ADMIN'] },
  { path: '/platform/tags', label: '推荐标签', eyebrow: '09', icon: CollectionTag, roles: ['PLATFORM_ADMIN'] },
  { path: '/platform/audits', label: '审计日志', eyebrow: '10', icon: Tickets, roles: ['PLATFORM_ADMIN'] },
]

const visibleMenu = computed(() => menuItems.filter((item) => auth.can(item.roles)))
const currentTitle = computed(() => String(route.meta.title || '工作台'))

async function logout() {
  await ElMessageBox.confirm('确认退出当前管理账号？', '退出登录', { type: 'warning' })
  await auth.logout()
  await router.replace({ name: 'login' })
}

function navigate(path: string) {
  mobileMenuOpen.value = false
  void router.push(path)
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar" :class="{ 'sidebar--open': mobileMenuOpen }" aria-label="管理导航">
      <div class="brand">
        <span class="brand__mark" aria-hidden="true"><DataAnalysis /></span>
        <div>
          <strong class="brand__name">校园搭子</strong>
          <span class="brand__caption">审核治理中心</span>
        </div>
      </div>
      <div class="sidebar__scope">
        <span class="sidebar__scope-label">当前范围</span>
        <strong>{{ auth.user?.campusName || (auth.user?.campusId ? `校园 ${auth.user.campusId}` : '全平台') }}</strong>
      </div>
      <nav class="nav">
        <button
          v-for="item in visibleMenu"
          :key="item.path"
          class="nav__item"
          :class="{ 'nav__item--active': route.path === item.path }"
          type="button"
          @click="navigate(item.path)"
        >
          <component :is="item.icon" class="nav__icon" aria-hidden="true" />
          <span class="nav__copy">
            <span>{{ item.label }}</span>
            <small>{{ item.eyebrow }}</small>
          </span>
        </button>
      </nav>
      <div class="sidebar__foot">
        <div class="sidebar__operator">
          <span class="operator__avatar" aria-hidden="true">{{ auth.user?.nickname?.slice(0, 1) || '管' }}</span>
          <div>
            <strong>{{ auth.user?.nickname || '管理员' }}</strong>
            <span>{{ auth.role === 'PLATFORM_ADMIN' ? '平台管理员' : '校园审核员' }}</span>
          </div>
        </div>
        <button class="logout" type="button" @click="logout">
          <span>退出当前账号</span>
          <span aria-hidden="true">↗</span>
        </button>
      </div>
    </aside>

    <div v-if="mobileMenuOpen" class="backdrop" aria-hidden="true" @click="mobileMenuOpen = false" />
    <section class="workspace">
      <header class="topbar">
        <button class="menu-button" type="button" aria-label="打开导航" @click="mobileMenuOpen = true">
          <Menu aria-hidden="true" />
        </button>
        <div class="topbar__context">
          <div class="breadcrumbs" aria-label="当前位置">
            <span>校园搭子</span><span aria-hidden="true">/</span><strong>运营工作台</strong>
          </div>
          <div class="topbar__title-row">
            <strong class="topbar__title">{{ currentTitle }}</strong>
            <span class="topbar__status"><i aria-hidden="true" />系统运行中</span>
          </div>
        </div>
        <div class="topbar__meta">
          <span class="topbar__clock">{{ auth.user?.campusName || (auth.user?.campusId ? `校园 ${auth.user.campusId}` : '全平台范围') }}</span>
          <span class="topbar__divider" aria-hidden="true" />
          <span class="topbar__role">{{ auth.role === 'PLATFORM_ADMIN' ? '平台管理员' : '校园审核员' }}</span>
        </div>
      </header>
      <main id="main-content" class="content" tabindex="-1">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<style scoped>
.shell { min-height: 100vh; background: var(--color-canvas); }
.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 30;
  display: flex;
  width: 216px;
  flex-direction: column;
  color: #edf4f1;
  background: #10231f;
  border-right: 1px solid rgb(255 255 255 / 8%);
}
.brand { display: flex; align-items: center; gap: 11px; padding: 22px 18px 20px; border-bottom: 1px solid rgb(255 255 255 / 9%); }
.brand__mark { display: grid; width: 36px; height: 36px; place-items: center; color: #10231f; font-weight: 800; background: #f0d89f; border-radius: 10px 3px 10px 3px; }
.brand__mark :deep(svg) { width: 18px; height: 18px; }
.brand__name, .brand__caption { display: block; }
.brand__name { font-size: 15px; letter-spacing: 0.06em; }
.brand__caption { margin-top: 3px; color: #93aaa2; font-size: 11px; }
.sidebar__scope { display: grid; gap: 5px; margin: 16px 16px 6px; padding: 11px 12px; color: #e6f0ec; background: rgb(255 255 255 / 6%); border: 1px solid rgb(255 255 255 / 8%); border-radius: 5px; }
.sidebar__scope-label { color: #78928a; font-size: 10px; }
.sidebar__scope strong { overflow: hidden; font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.nav { display: grid; min-height: 0; align-content: start; gap: 3px; padding: 11px 10px; overflow-y: auto; }
.nav__item { display: flex; align-items: center; gap: 11px; width: 100%; min-height: 44px; padding: 9px 11px; color: #acc0b8; text-align: left; background: transparent; border: 0; border-radius: 5px; cursor: pointer; transition: color .16s ease, background-color .16s ease, transform .16s ease; }
.nav__item:hover { color: #fff; background: rgb(255 255 255 / 7%); transform: translateX(1px); }
.nav__item--active { color: #fff; background: #254b40; box-shadow: inset 3px 0 #f0d89f; }
.nav__icon { flex: none; width: 17px; height: 17px; color: currentColor; }
.nav__copy { display: flex; align-items: center; justify-content: space-between; width: 100%; gap: 6px; font-size: 13px; }
.nav__copy small { color: #6f8d83; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 9px; }
.nav__item--active .nav__copy small { color: #bdcf9d; }
.sidebar__foot { display: grid; gap: 13px; margin-top: auto; padding: 16px 16px 18px; border-top: 1px solid rgb(255 255 255 / 9%); }
.sidebar__operator { display: flex; align-items: center; gap: 9px; min-width: 0; }
.sidebar__operator strong, .sidebar__operator > div span { display: block; }
.sidebar__operator strong { overflow: hidden; color: #f4f8f6; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.sidebar__operator div span { margin-top: 3px; color: #8da79e; font-size: 10px; }
.operator__avatar { display: grid; width: 29px; height: 29px; flex: none; place-items: center; color: #18342d; font-size: 12px; font-weight: 700; background: #d3e7bb; border-radius: 50%; }
.logout { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 7px 0 0; color: #a8bbb4; font-size: 11px; text-align: left; background: none; border: 0; border-top: 1px solid rgb(255 255 255 / 9%); cursor: pointer; }
.logout:hover { color: #fff; }
.workspace { min-height: 100vh; margin-left: 216px; }
.topbar { position: sticky; top: 0; z-index: 20; display: flex; min-height: 78px; align-items: center; gap: 18px; padding: 12px 30px; background: rgb(255 255 255 / 94%); border-bottom: 1px solid var(--color-border); backdrop-filter: blur(14px); }
.topbar__context { min-width: 0; }
.breadcrumbs { display: flex; align-items: center; gap: 8px; color: #84908b; font-size: 11px; }
.breadcrumbs strong { color: #52615a; font-weight: 600; }
.topbar__title-row { display: flex; align-items: center; gap: 12px; margin-top: 5px; }
.topbar__title { font-size: 19px; font-weight: 700; }
.topbar__status { display: inline-flex; align-items: center; gap: 5px; color: #71817a; font-size: 11px; }
.topbar__status i { width: 6px; height: 6px; background: #34a56c; border-radius: 50%; box-shadow: 0 0 0 3px rgb(52 165 108 / 14%); }
.topbar__meta { display: flex; align-items: center; gap: 12px; margin-left: auto; color: #66756e; font-size: 11px; white-space: nowrap; }
.topbar__divider { width: 1px; height: 16px; background: var(--color-border); }
.topbar__role { color: #31463d; font-weight: 600; }
.menu-button { display: none; width: 34px; height: 34px; padding: 7px; color: #2f4b41; background: #eef3ef; border: 1px solid var(--color-border); border-radius: 5px; }
.menu-button :deep(svg) { width: 18px; height: 18px; }
.content { padding: 24px 30px 34px; outline: none; }
.backdrop { display: none; }
@media (max-width: 900px) {
  .sidebar { transform: translateX(-100%); transition: transform 0.22s ease; }
  .sidebar--open { transform: translateX(0); }
  .workspace { margin-left: 0; }
  .menu-button { display: block; }
  .backdrop { position: fixed; inset: 0; z-index: 25; display: block; background: rgb(8 24 19 / 45%); }
  .topbar__meta { display: none; }
}
@media (max-width: 560px) {
  .content { padding: 20px 0; }
  .topbar { padding: 10px 14px; }
  .topbar__status { display: none; }
  .topbar__title { font-size: 16px; }
}
</style>
