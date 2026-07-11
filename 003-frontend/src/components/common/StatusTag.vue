<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel } from '@/utils/domain-labels'

const props = defineProps<{ status: string }>()

const colors: Record<string, string> = {
  DRAFT: 'default',
  PENDING_REVIEW: 'orange',
  ACTIVE: 'green',
  APPROVED: 'green',
  REJECTED: 'red',
  ARCHIVED: 'default',
  UNVERIFIED: 'default',
  SUSPENDED: 'red',
  MATCHED: 'blue',
  IN_PROGRESS: 'cyan',
  PENDING_ACCEPTANCE: 'gold',
  COMPLETED: 'green',
  CLOSED: 'purple',
  CANCELLED: 'default',
  APPLIED: 'blue',
  ACCEPTED: 'green',
  WITHDRAWN: 'default',
  ASSIGNED: 'blue',
  TERMINATED: 'red',
  DISABLED: 'red',
}

const label = computed(() => statusLabel(props.status))
const color = computed(() => colors[props.status] ?? 'default')
</script>

<template>
  <a-tag :color="color">{{ label }}</a-tag>
</template>
