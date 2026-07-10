<template>
  <section class="page">
    <h1 class="page-title">个人中心</h1>
    <n-card>
      <n-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-placement="left"
        label-width="90"
        @submit.prevent="save"
      >
        <n-form-item label="姓名"><n-input :value="auth.user?.realName" disabled /></n-form-item>
        <n-form-item label="用户名"><n-input :value="auth.user?.username" disabled /></n-form-item>
        <n-form-item label="手机号" path="phone"><n-input v-model:value="form.phone" autocomplete="tel" /></n-form-item>
        <n-form-item label="邮箱" path="email"><n-input v-model:value="form.email" autocomplete="email" /></n-form-item>
        <n-form-item label="学院"><n-input v-model:value="form.college" /></n-form-item>
        <n-form-item label="专业"><n-input v-model:value="form.major" /></n-form-item>
        <n-button attr-type="submit" type="primary" :loading="loading">保存</n-button>
      </n-form>
    </n-card>
  </section>
</template>

<script setup>
import { reactive, ref, watchEffect } from 'vue'
import { NButton, NCard, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import http from '../api/http'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ phone: '', email: '', college: '', major: '' })
const rules = {
  phone: { pattern: /^$|^1\d{10}$/, message: '请输入 11 位手机号', trigger: ['input', 'blur'] },
  email: { type: 'email', message: '请输入有效邮箱', trigger: ['input', 'blur'] }
}

watchEffect(() => {
  Object.assign(form, {
    phone: auth.user?.phone || '',
    email: auth.user?.email || '',
    college: auth.user?.college || '',
    major: auth.user?.major || ''
  })
})

async function save() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    auth.user = await http.put('/profile', form)
    auth.persistSession()
    message.success('已保存')
  } catch (error) {
    message.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>
