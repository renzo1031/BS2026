<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '../../api/modules'
import type { Item } from '../../types'

const loading = shallowRef(false)
const items = ref<Item[]>([])
const dialog = shallowRef(false)
const current = shallowRef<Item | null>(null)
const reviewForm = reactive({ result: 'APPROVED', reason: '' })

async function load() {
  loading.value = true
  try {
    items.value = (await adminApi.items({ status: 'PENDING_REVIEW', pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

function openReview(row: Item, result: 'APPROVED' | 'REJECTED') {
  current.value = row
  reviewForm.result = result
  reviewForm.reason = result === 'APPROVED' ? '信息完整，通过上架。' : ''
  dialog.value = true
}

async function submitReview() {
  if (!current.value) return
  await adminApi.review(current.value.id, reviewForm)
  ElMessage.success('审核完成')
  dialog.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">物品审核</h1>
    <el-table v-loading="loading" :data="items" row-key="id">
      <el-table-column prop="itemNo" label="编号" min-width="160" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="publisherName" label="发布人" width="120" />
      <el-table-column prop="publisherPhone" label="发布人电话" width="150" />
      <el-table-column prop="eventTime" label="发生时间" min-width="160" />
      <el-table-column prop="lastOperationTime" label="最后更新时间" min-width="160" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReview(row, 'APPROVED')">通过</el-button>
          <el-button link type="danger" @click="openReview(row, 'REJECTED')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="审核处理" width="520px">
      <el-form label-position="top">
        <el-form-item label="审核结果">
          <el-radio-group v-model="reviewForm.result">
            <el-radio-button label="APPROVED">通过</el-radio-button>
            <el-radio-button label="REJECTED">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核原因"><el-input v-model="reviewForm.reason" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitReview">提交</el-button></template>
    </el-dialog>
  </section>
</template>
