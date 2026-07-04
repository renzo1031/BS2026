<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/modules'
import type { AdminItemRow, OperationLog } from '../../types'

const loading = shallowRef(false)
const drawer = shallowRef(false)
const items = ref<AdminItemRow[]>([])
const total = shallowRef(0)
const filters = reactive({ keyword: '', status: '', type: '', pageNum: 1, pageSize: 10 })
const detail = ref<Record<string, unknown>>({})
const currentItem = computed(() => detail.value.item as AdminItemRow | undefined)
const timeline = computed(() => (detail.value.timeline || []) as OperationLog[])

async function load() {
  loading.value = true
  try {
    const page = await adminApi.items(filters)
    items.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

async function openDetail(row: AdminItemRow) {
  detail.value = await adminApi.itemDetail(row.id)
  drawer.value = true
}

async function askAction(row: AdminItemRow, action: 'offline' | 'archive') {
  const label = action === 'offline' ? '下架' : '归档'
  const { value } = await ElMessageBox.prompt(`请输入${label}原因`, label, { inputType: 'textarea' })
  if (action === 'offline') await adminApi.offline(row.id, { reason: value })
  if (action === 'archive') await adminApi.archive(row.id, { reason: value })
  ElMessage.success(`${label}完成`)
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">全量物品管理</h1>
    <div class="panel">
      <div class="toolbar">
        <el-input v-model="filters.keyword" placeholder="标题/编号" style="width: 220px" clearable />
        <el-select v-model="filters.type" placeholder="类型" clearable style="width: 130px">
          <el-option label="招领" value="FOUND" />
          <el-option label="寻物" value="LOST" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 160px">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已上架" value="PUBLISHED" />
          <el-option label="待交接" value="HANDOVER_PENDING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已归档" value="ARCHIVED" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="items" row-key="id">
        <el-table-column prop="itemNo" label="编号" min-width="160" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="publisherName" label="发布人" width="120" />
        <el-table-column prop="publisherPhone" label="发布人电话" width="150" />
        <el-table-column prop="claimantName" label="认领人" width="120" />
        <el-table-column prop="claimantPhone" label="认领人电话" width="150" />
        <el-table-column prop="reviewerName" label="审核人" width="120" />
        <el-table-column prop="custodianName" label="保管员" width="120" />
        <el-table-column prop="lastOperatorName" label="最后操作人" width="130" />
        <el-table-column prop="lastOperationTime" label="最后更新时间" min-width="160" />
        <el-table-column prop="status" label="状态" width="130" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="warning" @click="askAction(row, 'offline')">下架</el-button>
              <el-button link type="success" @click="askAction(row, 'archive')">归档</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="filters.pageNum" layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>

    <el-drawer v-model="drawer" title="物品详情与审计链" size="60%">
      <el-descriptions v-if="currentItem" border :column="1">
        <el-descriptions-item label="编号">{{ currentItem.itemNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentItem.title }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ currentItem.publisherName || currentItem.publisherId }}</el-descriptions-item>
        <el-descriptions-item label="发布人电话">{{ currentItem.publisherPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认领人">{{ currentItem.claimantName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认领人电话">{{ currentItem.claimantPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ currentItem.reviewerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="保管员">{{ currentItem.custodianName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后操作人">{{ currentItem.lastOperatorName || currentItem.lastOperatorId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后更新时间">{{ currentItem.lastOperationTime }}</el-descriptions-item>
        <el-descriptions-item label="最后操作摘要">{{ currentItem.lastOperationSummary }}</el-descriptions-item>
      </el-descriptions>
      <h3>状态时间线</h3>
      <el-timeline>
        <el-timeline-item v-for="log in timeline" :key="log.id" :timestamp="log.createdAt">
          {{ log.operatorName }} {{ log.action }}：{{ log.beforeStatus || '-' }} -> {{ log.afterStatus || '-' }}，{{ log.reason }}
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </section>
</template>
