<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { api } from '@/api/http'
import type { PublicOverview } from '@/types/models'
import HomeHero from '@/components/home/HomeHero.vue'
import ServiceBand from '@/components/home/ServiceBand.vue'
import ProcessBand from '@/components/home/ProcessBand.vue'
import PublicImpact from '@/components/home/PublicImpact.vue'

const overview = shallowRef<PublicOverview>({
  completedServices: 0,
  approvedVolunteers: 0,
  activeRequests: 0,
  serviceDepartments: 0,
})
const loading = shallowRef(true)

onMounted(async () => {
  try {
    overview.value = await api.get<PublicOverview>('/public/overview')
  } catch {
    // The homepage remains usable while the API is unavailable.
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <HomeHero />
  <ServiceBand />
  <ProcessBand />
  <PublicImpact :overview="overview" :loading="loading" />
  <section class="join-band">
    <div class="container join-inner">
      <div>
        <p>参与服务</p>
        <h2>有边界的善意，才能成为持续的帮助</h2>
      </div>
      <div class="join-actions">
        <a-button type="primary" size="large" href="/register">志愿者注册</a-button>
        <a-button size="large" href="/login">进入工作台</a-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.join-band {
  padding: 70px 0;
  background: #ffffff;
}

.join-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 36px;
}

.join-inner p {
  margin: 0 0 8px;
  color: #d9485f;
  font-weight: 700;
}

.join-inner h2 {
  margin: 0;
  font-size: 30px;
}

.join-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

@media (max-width: 700px) {
  .join-inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .join-actions {
    width: 100%;
  }

  .join-actions :deep(.ant-btn) {
    flex: 1;
  }
}
</style>
