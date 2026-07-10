<template>
  <main class="result-page">
    <n-result status="403" title="无权访问" description="当前账号没有访问此页面的权限。">
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
