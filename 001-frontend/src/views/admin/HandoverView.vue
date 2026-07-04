<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, claimApi } from '../../api/modules'
import type { AdminItemRow } from '../../types'

const loading = shallowRef(false)
const items = ref<AdminItemRow[]>([])

async function load() {
  loading.value = true
  try {
    items.value = (await adminApi.items({ status: 'HANDOVER_PENDING', pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

async function complete(row: AdminItemRow) {
  const { value } = await ElMessageBox.prompt('请输入交接地点', '完成交接')
  await claimApi.handover(row.id, { handoverLocation: value, remark: '线下交接完成' })
  ElMessage.success('交接完成')
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">保管交接</h1>
    <el-table v-loading="loading" :data="items" row-key="id">
      <el-table-column prop="itemNo" label="编号" min-width="160" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="claimantName" label="认领人" width="120" />
      <el-table-column prop="claimantPhone" label="认领人电话" width="150" />
      <el-table-column prop="custodianName" label="保管员" width="120" />
      <el-table-column prop="custodyLocation" label="保管位置" min-width="180" />
      <el-table-column prop="lastOperationTime" label="最后更新时间" min-width="160" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="complete(row)">完成交接</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
