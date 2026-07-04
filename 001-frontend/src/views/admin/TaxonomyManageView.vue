<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'
import type { Category, Location } from '../../types'

const categories = ref<Category[]>([])
const locations = ref<Location[]>([])
const loading = shallowRef(false)
const categoryForm = reactive({ categoryName: '', sortOrder: 0, status: 'ENABLED' })
const locationForm = reactive({ locationName: '', areaName: '', sortOrder: 0, status: 'ENABLED' })

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '停用'
  }
  return status ? labels[status] || status : '-'
}

function statusTagType(status?: string) {
  return status === 'ENABLED' ? 'success' : 'info'
}

async function load() {
  loading.value = true
  try {
    const [categoryPage, locationPage] = await Promise.all([
      adminApi.categories({ pageNum: 1, pageSize: 50 }),
      adminApi.locations({ pageNum: 1, pageSize: 50 })
    ])
    categories.value = categoryPage.records
    locations.value = locationPage.records
  } finally {
    loading.value = false
  }
}

async function saveCategory() {
  await adminApi.saveCategory(categoryForm)
  categoryForm.categoryName = ''
  ElMessage.success('分类已保存')
  await load()
}

async function saveLocation() {
  await adminApi.saveLocation(locationForm)
  locationForm.locationName = ''
  locationForm.areaName = ''
  ElMessage.success('地点已保存')
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">分类地点管理</h1>
    <div class="taxonomy-grid">
      <div class="panel">
        <h2 class="section-title">物品分类</h2>
        <div class="toolbar">
          <el-input v-model="categoryForm.categoryName" placeholder="分类名称" />
          <el-button type="primary" @click="saveCategory">新增</el-button>
        </div>
        <el-table v-loading="loading" :data="categories" row-key="id">
          <el-table-column prop="categoryName" label="分类" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <h2 class="section-title">校园地点</h2>
        <div class="toolbar">
          <el-input v-model="locationForm.locationName" placeholder="地点名称" />
          <el-input v-model="locationForm.areaName" placeholder="区域" />
          <el-button type="primary" @click="saveLocation">新增</el-button>
        </div>
        <el-table v-loading="loading" :data="locations" row-key="id">
          <el-table-column prop="locationName" label="地点" />
          <el-table-column prop="areaName" label="区域" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </section>
</template>

<style scoped>
.taxonomy-grid {
  width: 100%;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.taxonomy-grid > .panel {
  min-width: 0;
}

:deep(.el-table) {
  width: 100%;
}

@media (max-width: 1000px) {
  .taxonomy-grid {
    grid-template-columns: 1fr;
  }
}
</style>
