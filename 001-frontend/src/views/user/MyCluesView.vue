<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { clueApi } from '../../api/modules'
import type { Clue } from '../../types'

const loading = shallowRef(false)
const clues = ref<Clue[]>([])

async function load() {
  loading.value = true
  try {
    clues.value = (await clueApi.mine({ pageNum: 1, pageSize: 20 })).records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">我的线索</h1>
    <el-table v-loading="loading" :data="clues" row-key="id">
      <el-table-column prop="itemId" label="物品ID" width="100" />
      <el-table-column prop="clueContent" label="线索内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" width="150" />
      <el-table-column prop="status" label="状态" width="140" />
      <el-table-column prop="confirmReason" label="确认意见" min-width="180" />
    </el-table>
  </section>
</template>
