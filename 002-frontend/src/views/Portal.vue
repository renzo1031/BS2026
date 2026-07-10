<script setup>
import { computed, onMounted, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../store/auth'
import PortalActivity from '../components/portal/PortalActivity.vue'
import PortalHero from '../components/portal/PortalHero.vue'
import PortalProcess from '../components/portal/PortalProcess.vue'
import PortalServiceDirectory from '../components/portal/PortalServiceDirectory.vue'

const router = useRouter()
const auth = useAuthStore()
const categories = ref([])
const items = ref([])
const requests = ref([])
const notices = ref([])
const catalogLoading = shallowRef(false)
const activityLoading = shallowRef(false)
const catalogError = shallowRef('')
const activityError = shallowRef('')
const searchText = shallowRef('')

const userName = computed(() => auth.user?.realName || auth.user?.username || '同学')

onMounted(() => {
  loadCatalog()
  loadActivity()
})

async function loadCatalog() {
  catalogLoading.value = true
  catalogError.value = ''
  try {
    const [categoryData, itemData] = await Promise.all([
      http.get('/catalog/categories'),
      http.get('/catalog/items')
    ])
    categories.value = categoryData || []
    items.value = itemData || []
  } catch (error) {
    catalogError.value = error.message
  } finally {
    catalogLoading.value = false
  }
}

async function loadActivity() {
  activityLoading.value = true
  activityError.value = ''
  try {
    const [requestData, noticeData] = await Promise.all([
      http.get('/requests/my', { params: { page: 1, size: 3 } }),
      http.get('/notices/my')
    ])
    requests.value = requestData?.records || []
    const noticeList = Array.isArray(noticeData) ? noticeData : noticeData?.records || []
    notices.value = noticeList.slice(0, 4)
  } catch (error) {
    activityError.value = error.message
  } finally {
    activityLoading.value = false
  }
}

function searchServices(query) {
  searchText.value = query
  requestAnimationFrame(() => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    document.getElementById('service-directory')?.scrollIntoView({ behavior: reduceMotion ? 'auto' : 'smooth', block: 'start' })
  })
}

function startRequest(itemId) {
  router.push({ path: '/apply', query: itemId ? { itemId } : undefined })
}
</script>

<template>
  <div class="portal-home">
    <PortalHero :user-name="userName" @search="searchServices" />
    <PortalServiceDirectory
      :categories="categories"
      :items="items"
      :search-text="searchText"
      :loading="catalogLoading"
      :error-message="catalogError"
      @apply="startRequest"
      @retry="loadCatalog"
    />
    <PortalProcess />
    <PortalActivity
      :requests="requests"
      :notices="notices"
      :loading="activityLoading"
      :error-message="activityError"
      @open-request="(id) => router.push(`/requests/${id}`)"
      @open-requests="router.push('/my-requests')"
      @open-notices="router.push('/notices')"
      @start="startRequest()"
    />
  </div>
</template>

<style scoped>
.portal-home {
  min-width: 0;
  overflow: hidden;
}
</style>
