<template>
  <section class="page">
    <h1 class="page-title">消息通知</h1>

    <div v-if="loading" class="state-block"><n-spin size="large" /></div>
    <n-result v-else-if="errorMessage" status="error" title="通知加载失败" :description="errorMessage">
      <template #footer><n-button @click="load">重试</n-button></template>
    </n-result>
    <n-empty v-else-if="!notices.length" description="暂无消息通知" />
    <n-list v-else bordered>
      <n-list-item v-for="notice in notices" :key="notice.id">
        <n-thing :title="notice.title" :description="formatDateTime(notice.createdAt)">
          <p>{{ notice.content }}</p>
          <template #footer>
            <n-space>
              <n-button
                v-if="!isRead(notice)"
                size="small"
                :loading="pendingId === notice.id"
                @click="markRead(notice)"
              >标记已读</n-button>
              <n-button v-if="notice.requestId" size="small" type="primary" secondary @click="openRequest(notice)">查看申请</n-button>
            </n-space>
          </template>
        </n-thing>
      </n-list-item>
    </n-list>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NList, NListItem, NResult, NSpace, NSpin, NThing, useMessage } from 'naive-ui'
import http from '../api/http'
import { formatDateTime } from '../utils/display'

const router = useRouter()
const message = useMessage()
const notices = ref([])
const loading = ref(false)
const errorMessage = ref('')
const pendingId = ref(null)

onMounted(load)

function isRead(notice) {
  return notice.readFlag === 1 || notice.readFlag === true
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await http.get('/notices/my')
    notices.value = Array.isArray(result) ? result : result?.records || []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function markRead(notice) {
  pendingId.value = notice.id
  try {
    await http.post(`/notices/${notice.id}/read`)
    notice.readFlag = 1
  } catch (error) {
    message.error(error.message)
  } finally {
    pendingId.value = null
  }
}

async function openRequest(notice) {
  if (!isRead(notice)) await markRead(notice)
  router.push(`/requests/${notice.requestId}`)
}
</script>

<style scoped>
p {
  margin: 8px 0;
  white-space: pre-wrap;
}
</style>
