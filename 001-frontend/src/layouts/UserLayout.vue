<script setup lang="ts">
const menus = [
  { path: '/user', label: '个人资料', desc: '资料与账号安全' },
  { path: '/user/publish', label: '发布登记', desc: '登记遗失或拾到物品' },
  { path: '/user/items', label: '我的发布', desc: '查看审核与交接进度' },
  { path: '/user/claims', label: '我的认领', desc: '核验中的认领申请' },
  { path: '/user/clues', label: '我的线索', desc: '提交过的线索反馈' },
  { path: '/user/notices', label: '我的通知', desc: '公告与系统提醒' }
]
</script>

<template>
  <div class="user-layout page-shell">
    <aside class="side">
      <RouterLink to="/" class="back">返回门户</RouterLink>
      <div class="side-heading">
        <span class="side-kicker">Campus account</span>
        <h2 class="side-title">个人中心</h2>
        <p class="side-copy">管理资料、登记记录、认领进度和校园通知。</p>
      </div>
      <nav class="side-nav" aria-label="个人中心导航">
        <RouterLink v-for="menu in menus" :key="menu.path" :to="menu.path" class="side-link">
          <span class="side-link-label">{{ menu.label }}</span>
          <small class="side-link-desc">{{ menu.desc }}</small>
        </RouterLink>
      </nav>
    </aside>
    <section class="content">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.user-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.side,
.content {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.side {
  position: sticky;
  top: 18px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 520px;
  background:
    linear-gradient(180deg, rgba(27, 117, 187, 0.08), rgba(255, 255, 255, 0) 42%),
    #fff;
}

.side-heading {
  padding: 12px 10px 4px;
}

.side-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--brand);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.side-title {
  margin: 0;
  font-size: 24px;
}

.side-copy {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.side-link {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #344054;
}

.side-link.router-link-exact-active {
  color: #fff;
  background: linear-gradient(135deg, var(--brand), #2396d4);
  box-shadow: 0 10px 22px rgba(27, 117, 187, 0.18);
}

.side-link.router-link-exact-active .side-link-desc {
  color: rgba(255, 255, 255, 0.78);
}

.side-link-label {
  font-weight: 700;
}

.side-link-desc {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.4;
}

.back {
  width: fit-content;
  color: var(--brand) !important;
  font-weight: 700;
}

.content {
  min-width: 0;
  padding: 20px;
}

@media (max-width: 860px) {
  .user-layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
    min-height: auto;
  }

  .side-nav {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .side-nav {
    grid-template-columns: 1fr;
  }
}
</style>
