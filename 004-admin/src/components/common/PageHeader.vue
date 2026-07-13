<script setup lang="ts">
interface Props {
  title: string
  description?: string
}

defineProps<Props>()
</script>

<template>
  <header class="page-header">
    <div class="page-header__copy">
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 0 2px;
}

.page-header__copy {
  min-width: 0;
}

.page-header__title {
  position: relative;
  margin: 0;
  padding-left: 12px;
  color: var(--color-ink);
  font-size: clamp(20px, 2vw, 25px);
  font-weight: 700;
  line-height: 1.25;
}

.page-header__title::before {
  position: absolute;
  inset: 3px auto 3px 0;
  width: 3px;
  background: var(--color-primary);
  border-radius: 2px;
  content: '';
}

.page-header__description {
  max-width: 840px;
  margin: 6px 0 0 12px;
  color: var(--color-muted);
  font-size: 12px;
  line-height: 1.55;
}

.page-header__actions {
  display: flex;
  flex: none;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 640px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
    padding: 0 12px;
  }

  .page-header__actions { justify-content: flex-start; }
}
</style>
