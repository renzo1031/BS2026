<template>
  <main class="result-page">
    <n-result status="404" title="页面不存在" description="地址可能有误，或页面已经被移除。">
      <template #footer><n-button type="primary" @click="goHome">返回首页</n-button></template>
    </n-result>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { NButton, NResult } from 'naive-ui'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()

function goHome() {
  router.replace(auth.token ? auth.homePath : '/login')
}
</script>
