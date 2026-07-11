<script setup lang="ts">
import { reactive, useTemplateRef, watch } from 'vue'
import type { FormInstance } from 'ant-design-vue'
import type { ChildView } from '@/types/models'
import type { ChildFormPayload } from '@/types/forms'

const props = withDefaults(defineProps<{
  record?: ChildView | null
  loading?: boolean
}>(), { record: null, loading: false })
const emit = defineEmits<{ submit: [payload: ChildFormPayload] }>()
const open = defineModel<boolean>('open', { required: true })
const formRef = useTemplateRef<FormInstance>('formRef')
const form = reactive<ChildFormPayload>({
  name: '', gender: 'FEMALE', birthDate: '', region: '', schoolStage: '',
  guardianName: '', guardianPhone: '', address: '', familySummary: '', riskLevel: 'MEDIUM',
})

watch(open, (value) => {
  if (!value) return
  Object.assign(form, props.record ? {
    name: props.record.name,
    gender: props.record.gender,
    birthDate: props.record.birthDate,
    region: props.record.region,
    schoolStage: props.record.schoolStage,
    guardianName: props.record.guardianName,
    guardianPhone: props.record.guardianPhone,
    address: props.record.address,
    familySummary: props.record.familySummary,
    riskLevel: props.record.riskLevel,
    version: props.record.version,
  } : {
    name: '', gender: 'FEMALE', birthDate: '', region: '', schoolStage: '', guardianName: '',
    guardianPhone: '', address: '', familySummary: '', riskLevel: 'MEDIUM', version: undefined,
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

function validateBirthDate() {
  void formRef.value?.validateFields(['birthDate']).catch(() => undefined)
}
</script>

<template>
  <a-modal v-model:open="open" :title="record ? '编辑儿童档案' : '新建儿童档案'" :width="760" :confirm-loading="loading" @ok="submit">
    <a-form ref="formRef" :model="form" layout="vertical" class="child-form">
      <a-form-item label="姓名" name="name" :rules="[{ required: true, message: '请输入姓名' }]">
        <a-input v-model:value="form.name" :maxlength="30" autocomplete="off" />
      </a-form-item>
      <a-form-item label="性别" name="gender" :rules="[{ required: true }]">
        <a-select v-model:value="form.gender" :options="[{ value: 'MALE', label: '男' }, { value: 'FEMALE', label: '女' }, { value: 'OTHER', label: '其他' }]" />
      </a-form-item>
      <a-form-item label="出生日期" name="birthDate" :rules="[{ required: true, message: '请选择出生日期' }]">
        <input v-model="form.birthDate" class="native-date" type="date" required @change="validateBirthDate" />
      </a-form-item>
      <a-form-item label="学段" name="schoolStage" :rules="[{ required: true, message: '请选择学段' }]">
        <a-select v-model:value="form.schoolStage" :options="['学前','小学','初中','高中'].map(value => ({ value, label: value }))" />
      </a-form-item>
      <a-form-item label="服务区域" name="region" :rules="[{ required: true, message: '请输入服务区域' }]">
        <a-input v-model:value="form.region" :maxlength="100" />
      </a-form-item>
      <a-form-item label="风险等级" name="riskLevel" :rules="[{ required: true }]">
        <a-select v-model:value="form.riskLevel" :options="[{ value: 'LOW', label: '低' }, { value: 'MEDIUM', label: '中' }, { value: 'HIGH', label: '高' }]" />
      </a-form-item>
      <a-form-item label="监护人姓名" name="guardianName" :rules="[{ required: true, message: '请输入监护人姓名' }]">
        <a-input v-model:value="form.guardianName" :maxlength="40" autocomplete="off" />
      </a-form-item>
      <a-form-item label="监护人电话" name="guardianPhone" :rules="[{ required: true, pattern: /^1\d{10}$/, message: '请输入正确手机号' }]">
        <a-input v-model:value="form.guardianPhone" :maxlength="11" autocomplete="tel" />
      </a-form-item>
      <a-form-item class="wide" label="家庭地址" name="address" :rules="[{ required: true, message: '请输入家庭地址' }]">
        <a-input v-model:value="form.address" :maxlength="200" autocomplete="off" />
      </a-form-item>
      <a-form-item class="wide" label="家庭情况说明" name="familySummary" :rules="[{ required: true, message: '请输入家庭情况说明' }]">
        <a-textarea v-model:value="form.familySummary" :rows="4" :maxlength="1000" show-count />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.child-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.wide {
  grid-column: 1 / -1;
}

.native-date {
  width: 100%;
  height: 38px;
  padding: 4px 11px;
  color: #17212b;
  background: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.native-date:focus {
  border-color: #087f5b;
  outline: 2px solid rgba(8, 127, 91, 0.12);
}

@media (max-width: 620px) {
  .child-form {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
