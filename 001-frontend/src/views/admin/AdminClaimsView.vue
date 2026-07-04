<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, claimApi } from '../../api/modules'
import type { Claim } from '../../types'

const loading = shallowRef(false)
const claims = ref<Claim[]>([])
const dialog = shallowRef(false)
const current = shallowRef<Claim | null>(null)
const form = reactive({ reason: '证明信息匹配，等待线下交接。', custodyLocation: '失物招领中心' })

async function load() {
  loading.value = true
  try {
    claims.value = (await adminApi.claims({ pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

function open(row: Claim) {
  current.value = row
  dialog.value = true
}

async function approve() {
  if (!current.value) return
  await claimApi.approve(current.value.id, form)
  ElMessage.success('核验通过')
  dialog.value = false
  await load()
}

async function reject(row: Claim) {
  await claimApi.reject(row.id, { reason: '证明材料不足，驳回认领。' })
  ElMessage.success('已驳回')
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">认领核验</h1>
    <el-table v-loading="loading" :data="claims" row-key="id">
      <el-table-column prop="itemId" label="物品ID" width="100" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="applicantPhone" label="电话" width="150" />
      <el-table-column prop="proofText" label="证明材料" min-width="260" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="130" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="open(row)">通过</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="reject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="核验通过" width="520px">
      <el-form label-position="top">
        <el-form-item label="保管位置"><el-input v-model="form.custodyLocation" /></el-form-item>
        <el-form-item label="核验意见"><el-input v-model="form.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="approve">确认通过</el-button></template>
    </el-dialog>
  </section>
</template>
