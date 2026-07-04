<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { adminApi } from '../../api/modules'
import type { Clue } from '../../types'

const loading = shallowRef(false)
const clues = ref<Clue[]>([])

async function load() {
  loading.value = true
  try {
    clues.value = (await adminApi.clues({ pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    PENDING_CONFIRM: '待确认',
    VALID: '有效线索',
    INVALID: '无效线索'
  }
  return status ? labels[status] || status : '-'
}

function statusTagType(status?: string) {
  const types: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING_CONFIRM: 'warning',
    VALID: 'success',
    INVALID: 'danger'
  }
  return status ? types[status] || 'info' : 'info'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">线索管理</h1>
    <el-table v-loading="loading" :data="clues" row-key="id">
      <el-table-column label="物品" min-width="180">
        <template #default="{ row }">
          <div class="stack-cell">
            <strong>{{ row.itemTitle || '-' }}</strong>
            <span>{{ row.itemNo || `#${row.itemId}` }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="150">
        <template #default="{ row }">
          <div class="stack-cell">
            <strong>{{ row.submitterName || '-' }}</strong>
            <span>{{ row.submitterPhone || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="clueContent" label="线索内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" width="150" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.stack-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  line-height: 1.45;
}

.stack-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stack-cell span {
  color: var(--muted);
  font-size: 12px;
}
</style>
