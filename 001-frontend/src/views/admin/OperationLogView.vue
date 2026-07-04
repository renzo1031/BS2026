<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { adminApi } from '../../api/modules'
import type { OperationLog } from '../../types'

const loading = shallowRef(false)
const logs = ref<OperationLog[]>([])
const filters = reactive({ targetType: '', action: '', operatorId: '', pageNum: 1, pageSize: 20 })
const total = shallowRef(0)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.logs(filters)
    logs.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">操作日志</h1>
    <div class="panel">
      <div class="toolbar">
        <el-input v-model="filters.operatorId" placeholder="操作人ID" style="width: 140px" clearable />
        <el-select v-model="filters.targetType" placeholder="对象类型" clearable style="width: 150px">
          <el-option label="物品" value="ITEM" />
          <el-option label="认领" value="CLAIM" />
          <el-option label="线索" value="CLUE" />
        </el-select>
        <el-input v-model="filters.action" placeholder="动作" style="width: 180px" clearable />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="logs" row-key="id">
        <el-table-column prop="createdAt" label="时间" min-width="160" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operatorRole" label="角色" width="100" />
        <el-table-column prop="targetType" label="对象" width="100" />
        <el-table-column prop="targetId" label="对象ID" width="100" />
        <el-table-column prop="action" label="动作" min-width="150" />
        <el-table-column label="状态变化" min-width="180">
          <template #default="{ row }">{{ row.beforeStatus || '-' }} -> {{ row.afterStatus || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="IP" width="120" />
        <el-table-column prop="requestPath" label="路径" min-width="180" show-overflow-tooltip />
      </el-table>
      <el-pagination v-model:current-page="filters.pageNum" layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>
  </section>
</template>
