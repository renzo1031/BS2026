<script setup lang="ts">
interface ProfileStat {
  label: string
  value: number
  description: string
  to: string
  tone: 'blue' | 'green' | 'amber' | 'gray'
}

defineProps<{
  stats: ProfileStat[]
}>()
</script>

<template>
  <section class="profile-stats" aria-label="个人业务统计">
    <RouterLink v-for="stat in stats" :key="stat.label" :to="stat.to" class="stat-card" :class="`tone-${stat.tone}`">
      <span class="stat-label">{{ stat.label }}</span>
      <strong class="stat-value">{{ stat.value }}</strong>
      <span class="stat-desc">{{ stat.description }}</span>
    </RouterLink>
  </section>
</template>

<style scoped>
.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  min-height: 132px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.stat-card::after {
  position: absolute;
  right: -28px;
  bottom: -32px;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  content: "";
  opacity: 0.12;
}

.tone-blue::after {
  background: var(--brand);
}

.tone-green::after {
  background: var(--green);
}

.tone-amber::after {
  background: var(--sun);
}

.tone-gray::after {
  background: #52677c;
}

.stat-label,
.stat-desc {
  display: block;
}

.stat-label {
  color: #52677c;
  font-size: 13px;
  font-weight: 700;
}

.stat-value {
  display: block;
  margin: 8px 0 6px;
  color: #132f4b;
  font-size: 34px;
  line-height: 1;
}

.stat-desc {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 980px) {
  .profile-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .profile-stats {
    grid-template-columns: 1fr;
  }
}
</style>
