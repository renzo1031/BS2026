<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <section class="route-state" role="alert" aria-labelledby="forbidden-title">
    <span class="route-state__code" aria-hidden="true">403</span>
    <h1 id="forbidden-title">当前账号无权访问</h1>
    <p>该页面仅对具备相应管理权限的账号开放。如职责已调整，请重新登录后再试。</p>
    <ElButton type="primary" @click="router.replace({ name: 'dashboard' })">返回工作台</ElButton>
  </section>
</template>

<style scoped>
.route-state { display: grid; min-height: 60vh; place-content: center; justify-items: start; max-width: 640px; gap: 14px; }
.route-state__code { color: var(--color-primary); font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 14px; letter-spacing: 0.16em; }
.route-state h1 { margin: 0; font-size: clamp(30px, 4vw, 48px); }
.route-state p { max-width: 560px; margin: 0 0 8px; color: var(--color-muted); line-height: 1.8; }
</style>
