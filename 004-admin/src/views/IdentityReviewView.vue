<script setup lang="ts">
import { onMounted, shallowRef, useTemplateRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { errorMessage, request } from '@/api/http'
import type { IdentityReview } from '@/types/api'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import IdentityReviewList from '@/components/reviews/IdentityReviewList.vue'

const listRef = useTemplateRef<InstanceType<typeof IdentityReviewList>>('list')
const rows = shallowRef<IdentityReview[]>([])
const loading = shallowRef(false)
const busy = shallowRef(false)
const error = shallowRef('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await request<IdentityReview[]>({ url: '/review/identity-bindings' })
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function decide(payload: { item: IdentityReview; approved: boolean; reason: string }) {
  busy.value = true
  try {
    await request({
      method: 'POST',
      url: `/review/identity-bindings/${payload.item.binding.id}/decision`,
      data: { version: payload.item.binding.version, approved: payload.approved, reason: payload.reason || undefined },
    })
    ElMessage.success(payload.approved ? '校园身份已认证' : '认证材料已驳回')
    listRef.value?.close()
    await load()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  } finally {
    busy.value = false
  }
}

async function openProof(item: IdentityReview) {
  if (!item.binding.proofFileId) return
  try {
    const result = await request<{ url: string }>({ url: `/files/${item.binding.proofFileId}/url` })
    window.open(result.url, '_blank', 'noopener,noreferrer')
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader title="校园身份审核" description="核对校园唯一标识与证明材料，敏感信息仅用于本次审核。">
      <template #actions><ElButton :icon="Refresh" :loading="loading" @click="load">刷新队列</ElButton></template>
    </PageHeader>
    <section class="page-section review-queue">
      <header class="review-queue__toolbar">
        <div><h2>待核验队列</h2><p>逐项比对标识和证明材料，不要复制或外传。</p></div>
        <span>共 {{ rows.length }} 项</span>
      </header>
      <AsyncState :loading="loading" :error="error" :empty="rows.length === 0" empty-text="当前没有待核验身份" @retry="load">
        <IdentityReviewList ref="list" :rows="rows" :busy="busy" @decide="decide" @open-proof="openProof" />
      </AsyncState>
    </section>
  </div>
</template>

<style scoped>
.review-queue { padding: 0; overflow: hidden; }
.review-queue__toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 18px; background: #fafaf8; border-bottom: 1px solid var(--color-border); }
.review-queue__toolbar h2 { margin: 0; font-size: 14px; }
.review-queue__toolbar p { margin: 4px 0 0; color: var(--color-muted); font-size: 11px; }
.review-queue__toolbar > span { flex: none; color: var(--color-muted); font-size: 12px; }

@media (max-width: 640px) {
  .review-queue__toolbar { align-items: flex-start; padding: 13px 12px; }
}
</style>
