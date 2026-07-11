<script setup lang="ts">
import { ArrowLeftOutlined, HeartFilled } from '@ant-design/icons-vue'
</script>

<template>
  <div class="auth-layout">
    <RouterLink class="back-home" to="/">
      <ArrowLeftOutlined /> 返回官网
    </RouterLink>
    <main class="auth-main">
      <div class="auth-brand" aria-label="童伴关爱">
        <span class="auth-mark"><HeartFilled /></span>
        <strong>童伴关爱</strong>
      </div>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.auth-layout {
  min-height: 100vh;
  padding: 24px;
  background: #edf5f2;
}

.back-home {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #345047;
  text-decoration: none;
}

.auth-main {
  width: min(100%, 430px);
  margin: 7vh auto 0;
}

.auth-brand {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  color: #143c32;
  font-size: 22px;
}

.auth-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: #087f5b;
  border-radius: 6px;
}
</style>
