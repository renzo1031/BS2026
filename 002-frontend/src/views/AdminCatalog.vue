<template>
  <section class="page">
    <h1 class="page-title">事项与场地</h1>

    <div v-if="loading" class="state-block"><n-spin size="large" /></div>
    <n-result v-else-if="errorMessage" status="error" title="基础数据加载失败" :description="errorMessage">
      <template #footer><n-button @click="load">重试</n-button></template>
    </n-result>
    <n-tabs v-else type="line" animated>
      <n-tab-pane name="items" tab="服务事项">
        <n-data-table :columns="itemColumns" :data="items" :pagination="false" :scroll-x="900" :row-key="(row) => row.id">
          <template #empty><n-empty description="暂无服务事项" /></template>
        </n-data-table>
      </n-tab-pane>
      <n-tab-pane name="venues" tab="活动场地">
        <n-data-table :columns="venueColumns" :data="venues" :pagination="false" :scroll-x="900" :row-key="(row) => row.id">
          <template #empty><n-empty description="暂无活动场地" /></template>
        </n-data-table>
      </n-tab-pane>
    </n-tabs>
  </section>
</template>

<script setup>
import { h, onMounted, ref } from 'vue'
import { NButton, NDataTable, NEmpty, NResult, NSpin, NSwitch, NTabPane, NTabs, NTag, useMessage } from 'naive-ui'
import http from '../api/http'
import { itemTypeMap } from '../utils/display'

const message = useMessage()
const items = ref([])
const venues = ref([])
const loading = ref(false)
const errorMessage = ref('')
const pendingKey = ref('')

const itemColumns = [
  { title: '事项名称', key: 'name', minWidth: 170 },
  { title: '编码', key: 'code', width: 150 },
  { title: '类型', key: 'type', width: 120, render: (row) => itemTypeMap[row.type] || row.type },
  { title: '所属部门', key: 'departmentName', width: 150, render: (row) => row.departmentName || '-' },
  { title: '说明', key: 'description', minWidth: 260, ellipsis: { tooltip: true } },
  {
    title: '启用状态', key: 'enabled', width: 110, fixed: 'right',
    render: (row) => h(NSwitch, {
      value: isEnabled(row.enabled),
      loading: pendingKey.value === `item-${row.id}`,
      'onUpdate:value': (value) => toggleItem(row, value)
    }, { checked: () => '启用', unchecked: () => '停用' })
  }
]

const venueColumns = [
  { title: '场地名称', key: 'name', minWidth: 180 },
  { title: '位置', key: 'location', minWidth: 180 },
  { title: '容量', key: 'capacity', width: 100, render: (row) => `${row.capacity} 人` },
  { title: '说明', key: 'description', minWidth: 260, ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'status', width: 100,
    render: (row) => h(NTag, { type: venueAvailable(row.status) ? 'success' : 'error', bordered: false }, {
      default: () => venueAvailable(row.status) ? '可用' : '停用'
    })
  },
  {
    title: '可预约', key: 'available', width: 100, fixed: 'right',
    render: (row) => h(NSwitch, {
      value: venueAvailable(row.status),
      loading: pendingKey.value === `venue-${row.id}`,
      'onUpdate:value': (value) => toggleVenue(row, value)
    })
  }
]

onMounted(load)

function isEnabled(value) {
  return value === true || value === 1 || value === 'ENABLED'
}

function venueAvailable(status) {
  return status === 'AVAILABLE' || status === 'ENABLED' || status === true || status === 1
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [itemData, venueData] = await Promise.all([
      http.get('/admin/service-items'),
      http.get('/admin/venues')
    ])
    items.value = itemData || []
    venues.value = venueData || []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function toggleItem(row, enabled) {
  pendingKey.value = `item-${row.id}`
  try {
    await http.patch(`/admin/service-items/${row.id}/enabled`, { enabled })
    row.enabled = enabled
    message.success(enabled ? '事项已上架' : '事项已下架')
  } catch (error) {
    message.error(error.message)
  } finally {
    pendingKey.value = ''
  }
}

async function toggleVenue(row, available) {
  const status = available ? 'AVAILABLE' : 'UNAVAILABLE'
  pendingKey.value = `venue-${row.id}`
  try {
    await http.patch(`/admin/venues/${row.id}/status`, { status })
    row.status = status
    message.success(available ? '场地已启用' : '场地已停用')
  } catch (error) {
    message.error(error.message)
  } finally {
    pendingKey.value = ''
  }
}
</script>
