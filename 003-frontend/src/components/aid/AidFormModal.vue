<script setup lang="ts">
import { reactive, useTemplateRef, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import type { AidView, ChildView } from '@/types/models'
import type { AidFormPayload } from '@/types/forms'

const props = withDefaults(defineProps<{
  record?: AidView | null
  children: ChildView[]
  loading?: boolean
}>(), { record: null, loading: false })
const emit = defineEmits<{ submit: [payload: AidFormPayload] }>()
const open = defineModel<boolean>('open', { required: true })
const formRef = useTemplateRef<FormInstance>('formRef')
const form = reactive<AidFormPayload>({
  childId: 0, category: 'EDUCATION', title: '', description: '', publicSummary: '', priority: 'NORMAL',
})

const categories = [
  { value: 'EDUCATION', label: '学习支持' },
  { value: 'COMPANIONSHIP', label: '成长陪伴' },
  { value: 'LIFE_CARE', label: '生活关怀' },
  { value: 'SAFETY', label: '安全关爱' },
  { value: 'PSYCHOLOGICAL', label: '心理支持' },
  { value: 'OTHER', label: '其他支持' },
]

watch(open, (value) => {
  if (!value) return
  Object.assign(form, props.record ? {
    childId: props.record.childId,
    category: props.record.category,
    title: props.record.title,
    description: props.record.description,
    publicSummary: props.record.publicSummary,
    priority: props.record.priority,
    version: props.record.version,
  } : {
    childId: props.children[0]?.id ?? 0,
    category: 'EDUCATION', title: '', description: '', publicSummary: '', priority: 'NORMAL', version: undefined,
  })
})

async function submit() {
  try {
    await formRef.value?.validateFields()
  } catch {
    return
  }
  emit('submit', { ...form })
}
</script>

<template>
  <a-modal v-model:open="open" :title="record ? '编辑帮扶需求' : '新建帮扶需求'" :width="760" :confirm-loading="loading" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical" class="aid-form">
      <a-form-item class="wide" label="儿童档案" name="childId" :rules="[{ required: true, type: 'number', min: 1, message: '请选择有效儿童档案' }]">
        <a-select
          v-model:value="form.childId"
          show-search
          option-filter-prop="label"
          :options="children.map(child => ({ value: child.id, label: `${child.fileNo} · ${child.name}` }))"
          placeholder="选择已审核通过的儿童档案"
        />
      </a-form-item>
      <a-form-item label="服务类型" name="category" :rules="[{ required: true }]">
        <a-select v-model:value="form.category" :options="categories" />
      </a-form-item>
      <a-form-item label="优先级" name="priority" :rules="[{ required: true }]">
        <a-radio-group v-model:value="form.priority">
          <a-radio value="NORMAL">普通</a-radio>
          <a-radio value="URGENT">紧急</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item class="wide" label="需求标题" name="title" :rules="[{ required: true, message: '请输入需求标题' }]">
        <a-input v-model:value="form.title" :maxlength="120" />
      </a-form-item>
      <a-form-item class="wide" label="内部情况说明" name="description" :rules="[{ required: true, message: '请输入内部情况说明' }]">
        <a-textarea v-model:value="form.description" :rows="5" :maxlength="2000" show-count />
      </a-form-item>
      <a-form-item class="wide" label="公开摘要" name="publicSummary" :rules="[{ required: true, message: '请输入公开摘要' }]">
        <a-textarea v-model:value="form.publicSummary" :rows="3" :maxlength="300" show-count placeholder="仅描述服务需要，不填写姓名、电话、证件号或详细地址" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.aid-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.wide {
  grid-column: 1 / -1;
}

@media (max-width: 620px) {
  .aid-form {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
