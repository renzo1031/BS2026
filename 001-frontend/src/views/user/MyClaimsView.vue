<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { claimApi } from '../../api/modules'
import type { Claim } from '../../types'

const loading = shallowRef(false)
const claims = ref<Claim[]>([])

async function load() {
  loading.value = true
  try {
    claims.value = (await claimApi.mine({ pageNum: 1, pageSize: 20 })).records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">我的认领</h1>
    <el-table v-loading="loading" :data="claims" row-key="id">
      <el-table-column prop="itemId" label="物品ID" width="100" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="applicantPhone" label="联系电话" width="150" />
      <el-table-column prop="proofText" label="证明材料" min-width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="reviewReason" label="审核意见" min-width="180" />
    </el-table>
  </section>
</template>
