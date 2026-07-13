<script setup lang="ts">
const title = import.meta.env.VITE_APP_TITLE || '校园搭子运营台'
</script>

<template>
  <a class="skip-link" href="#main-content">跳到主要内容</a>
  <RouterView />
  <span class="sr-only" aria-live="polite">{{ title }}</span>
</template>
