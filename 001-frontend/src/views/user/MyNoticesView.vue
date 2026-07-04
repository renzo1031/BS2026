<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { catalogApi } from '../../api/modules'
import type { Notice } from '../../types'

const notices = ref<Notice[]>([])

onMounted(async () => {
  notices.value = await catalogApi.notices()
})
</script>

<template>
  <section>
    <h1 class="section-title">我的通知</h1>
    <el-empty v-if="notices.length === 0" description="暂无通知" />
    <div v-for="notice in notices" :key="notice.id" class="panel notice">
      <strong>{{ notice.title }}</strong>
      <p>{{ notice.content }}</p>
      <span class="muted">{{ notice.publishedAt }}</span>
    </div>
  </section>
</template>

<style scoped>
.notice {
  margin-bottom: 12px;
}
</style>
