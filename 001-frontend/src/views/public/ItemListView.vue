<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { catalogApi, itemApi } from '../../api/modules'
import type { Category, Item, Location } from '../../types'

const router = useRouter()
const route = useRoute()
const loading = shallowRef(false)
const items = ref<Item[]>([])
const total = shallowRef(0)
const categories = ref<Category[]>([])
const locations = ref<Location[]>([])
const filters = reactive({
  keyword: queryText('keyword'),
  type: queryText('type'),
  categoryId: queryText('categoryId'),
  locationId: queryText('locationId'),
  pageNum: 1,
  pageSize: 10
})

function queryText(key: string) {
  const value = route.query[key]
  if (Array.isArray(value)) return value[0] || ''
  return value ? String(value) : ''
}

async function load() {
  loading.value = true
  try {
    const page = await itemApi.page(filters)
    items.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function init() {
  const [categoryList, locationList] = await Promise.all([catalogApi.categories(), catalogApi.locations()])
  categories.value = categoryList
  locations.value = locationList
  await load()
}

onMounted(init)
</script>

<template>
  <section class="page-shell">
    <h1 class="section-title">失物招领</h1>
    <div class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="搜索标题或描述" style="width: 220px" clearable />
        <el-select v-model="filters.type" placeholder="类型" clearable style="width: 140px">
          <el-option label="招领" value="FOUND" />
          <el-option label="寻物" value="LOST" />
        </el-select>
        <el-select v-model="filters.categoryId" placeholder="分类" clearable style="width: 160px">
          <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="String(item.id)" />
        </el-select>
        <el-select v-model="filters.locationId" placeholder="地点" clearable style="width: 160px">
          <el-option v-for="item in locations" :key="item.id" :label="item.locationName" :value="String(item.id)" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="router.push('/user/publish')">发布登记</el-button>
      </div>

      <el-table v-loading="loading" :data="items" row-key="id">
        <el-table-column prop="itemNo" label="编号" min-width="160" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ row.type === 'FOUND' ? '招领' : '寻物' }}</template>
        </el-table-column>
        <el-table-column prop="eventTime" label="时间" min-width="160" />
        <el-table-column prop="lastOperationSummary" label="状态摘要" min-width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/items/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="filters.pageNum"
        v-model:page-size="filters.pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>
  </section>
</template>
