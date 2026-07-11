<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { ReviewPayload } from '@/types/forms'

const props = withDefaults(defineProps<{
  title?: string
  loading?: boolean
  version?: number
}>(), { title: '审核', loading: false, version: undefined })
const emit = defineEmits<{ submit: [payload: ReviewPayload] }>()
const open = defineModel<boolean>('open', { required: true })
const form = reactive<ReviewPayload>({ decision: 'APPROVED', comment: '', version: props.version })

watch(open, (value) => {
  if (value) Object.assign(form, { decision: 'APPROVED', comment: '', version: props.version })
})

function submit() {
  if (form.decision === 'REJECTED' && !form.comment?.trim()) return
  emit('submit', { ...form, comment: form.comment?.trim() || undefined, version: props.version })
}
</script>

<template>
  <a-modal v-model:open="open" :title="title" :confirm-loading="loading" @ok="submit">
    <a-form layout="vertical">
      <a-form-item label="审核结果" required>
        <a-radio-group v-model:value="form.decision">
          <a-radio value="APPROVED">通过</a-radio>
          <a-radio value="REJECTED">驳回</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="审核意见" :required="form.decision === 'REJECTED'">
        <a-textarea v-model:value="form.comment" :rows="4" :maxlength="500" show-count />
        <p v-if="form.decision === 'REJECTED' && !form.comment?.trim()" class="error-text">驳回时必须填写原因</p>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.error-text {
  margin: 6px 0 0;
  color: #d9485f;
  font-size: 12px;
}
</style>
