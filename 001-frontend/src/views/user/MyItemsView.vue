<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { itemApi } from '../../api/modules'
import type { Item } from '../../types'

const loading = shallowRef(false)
const items = ref<Item[]>([])
const total = shallowRef(0)
const pageNum = shallowRef(1)

async function load() {
  loading.value = true
  try {
    const page = await itemApi.mine({ pageNum: pageNum.value, pageSize: 10 })
    items.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function submit(row: Item) {
  await itemApi.submit(row.id)
  ElMessage.success('已提交审核')
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">我的发布</h1>
    <el-table v-loading="loading" :data="items" row-key="id">
      <el-table-column prop="itemNo" label="编号" min-width="160" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="lastOperationSummary" label="最后操作" min-width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="['DRAFT', 'REJECTED'].includes(row.status)" link type="primary" @click="submit(row)">提交审核</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pageNum" layout="total, prev, pager, next" :total="total" @current-change="load" />
  </section>
</template>
