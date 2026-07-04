<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { claimApi, clueApi, itemApi } from '../../api/modules'
import { useAuthStore } from '../../stores/auth'
import type { Item, OperationLog } from '../../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(false)
const claimDialog = shallowRef(false)
const clueDialog = shallowRef(false)
const detail = ref<Record<string, unknown>>({})
const claimForm = reactive({ applicantName: '', applicantPhone: '', proofText: '' })
const clueForm = reactive({ clueContent: '', contactPhone: '' })
const item = computed(() => detail.value.item as Item | undefined)
const timeline = computed(() => (detail.value.timeline || []) as OperationLog[])

async function load() {
  loading.value = true
  try {
    detail.value = await itemApi.detail(Number(route.params.id))
  } finally {
    loading.value = false
  }
}

async function submitClaim() {
  if (!auth.isAuthed) {
    router.push('/login')
    return
  }
  await claimApi.create(Number(route.params.id), claimForm)
  ElMessage.success('认领申请已提交')
  claimDialog.value = false
  await load()
}

async function submitClue() {
  if (!auth.isAuthed) {
    router.push('/login')
    return
  }
  await clueApi.create(Number(route.params.id), clueForm)
  ElMessage.success('线索已提交')
  clueDialog.value = false
}

onMounted(load)
</script>

<template>
  <section class="page-shell">
    <el-skeleton v-if="loading" :rows="8" animated />
    <div v-else-if="item" class="detail-grid">
      <article class="panel">
        <div class="status-line">
          <el-tag>{{ item.type === 'FOUND' ? '招领' : '寻物' }}</el-tag>
          <el-tag type="success">{{ item.status }}</el-tag>
          <span class="muted">{{ item.itemNo }}</span>
        </div>
        <h1>{{ item.title }}</h1>
        <p>{{ item.description }}</p>
        <el-descriptions border :column="1">
          <el-descriptions-item label="发生时间">{{ item.eventTime }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ item.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ item.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ item.lastOperationTime || item.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="状态摘要">{{ item.lastOperationSummary }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button v-if="item.type === 'FOUND'" type="primary" @click="claimDialog = true">提交认领</el-button>
          <el-button @click="clueDialog = true">提供线索</el-button>
        </div>
      </article>

      <aside class="panel">
        <h2 class="section-title">状态时间线</h2>
        <el-timeline>
          <el-timeline-item v-for="log in timeline" :key="log.id" :timestamp="log.createdAt">
            {{ log.operatorName || '系统' }} {{ log.action }}：{{ log.beforeStatus || '-' }} -> {{ log.afterStatus || '-' }}
          </el-timeline-item>
        </el-timeline>
      </aside>
    </div>

    <el-dialog v-model="claimDialog" title="提交认领申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="申请人姓名"><el-input v-model="claimForm.applicantName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="claimForm.applicantPhone" /></el-form-item>
        <el-form-item label="证明材料"><el-input v-model="claimForm.proofText" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitClaim">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="clueDialog" title="提交线索" width="520px">
      <el-form label-position="top">
        <el-form-item label="线索内容"><el-input v-model="clueForm.clueContent" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="clueForm.contactPhone" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitClue">提交</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
}

.panel h1 {
  font-size: 30px;
  margin: 16px 0 10px;
}

.detail-actions {
  margin-top: 18px;
  display: flex;
  gap: 12px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
