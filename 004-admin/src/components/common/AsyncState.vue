<script setup lang="ts">
interface Props {
  loading: boolean
  error?: string
  empty?: boolean
  emptyText?: string
}

defineProps<Props>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="loading" class="state" role="status" aria-live="polite">
    <span class="state__spinner" aria-hidden="true" />
    <span class="state__label">正在加载…</span>
  </div>
  <div v-else-if="error" class="state state--error" role="alert">
    <span class="state__error-mark" aria-hidden="true">!</span>
    <strong>加载失败</strong>
    <span class="state__detail">{{ error }}</span>
    <ElButton size="small" @click="$emit('retry')">重新加载</ElButton>
  </div>
  <div v-else-if="empty" class="state" role="status">
    <span class="state__empty-mark" aria-hidden="true">—</span>
    <span class="state__label">{{ emptyText || '暂无数据' }}</span>
  </div>
  <slot v-else />
</template>

<style scoped>
.state {
  display: grid;
  min-height: 190px;
  place-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--color-muted);
  font-size: 13px;
  text-align: center;
}

.state--error {
  min-height: 160px;
  color: var(--color-danger);
}

.state__label { color: var(--color-muted); }
.state__detail { max-width: min(520px, 88vw); color: var(--color-muted); font-size: 12px; line-height: 1.5; }

.state__spinner {
  width: 22px;
  height: 22px;
  border: 2px solid #dce6e1;
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.state__empty-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #8fa29a;
  font-size: 24px;
  line-height: 1;
  background: #edf2ee;
  border-radius: 50%;
}

.state__error-mark {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  color: #fff;
  font-weight: 700;
  background: var(--color-danger);
  border-radius: 50%;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
