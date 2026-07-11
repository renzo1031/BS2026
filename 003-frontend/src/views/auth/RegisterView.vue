<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'

const router = useRouter()
const loading = shallowRef(false)
const form = reactive({ username: '', displayName: '', password: '', confirmPassword: '' })

async function submit() {
  if (form.password !== form.confirmPassword) {
    message.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await api.post<void>('/auth/register', {
      username: form.username,
      displayName: form.displayName,
      password: form.password,
    })
    message.success('注册成功，请登录并完成志愿者认证')
    await router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-panel">
    <h1>志愿者注册</h1>
    <p>注册后需完善服务资料并通过平台审核。</p>
    <a-form :model="form" layout="vertical" @finish="submit">
      <a-form-item label="用户名" name="username" :rules="[{ required: true, pattern: /^[A-Za-z0-9_]{4,32}$/, message: '请输入4到32位字母、数字或下划线' }]">
        <a-input v-model:value="form.username" size="large" autocomplete="username" />
      </a-form-item>
      <a-form-item label="显示名称" name="displayName" :rules="[{ required: true, message: '请输入显示名称' }]">
        <a-input v-model:value="form.displayName" size="large" autocomplete="name" />
      </a-form-item>
      <a-form-item label="密码" name="password" :rules="[{ required: true, min: 8, pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '至少8位并同时包含字母和数字' }]">
        <a-input-password v-model:value="form.password" size="large" autocomplete="new-password" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword" :rules="[{ required: true, message: '请再次输入密码' }]">
        <a-input-password v-model:value="form.confirmPassword" size="large" autocomplete="new-password" />
      </a-form-item>
      <a-button type="primary" html-type="submit" size="large" block :loading="loading">注册</a-button>
    </a-form>
    <p class="auth-switch">已有账号？<RouterLink to="/login">返回登录</RouterLink></p>
  </section>
</template>

<style scoped>
.auth-panel {
  padding: 30px;
  background: #ffffff;
  border: 1px solid #d8e1dd;
  border-radius: 6px;
  box-shadow: 0 12px 38px rgba(35, 70, 59, 0.1);
}

.auth-panel h1 {
  margin: 0;
  font-size: 26px;
}

.auth-panel > p {
  margin: 8px 0 24px;
  color: #69766f;
}

.auth-switch {
  margin: 22px 0 0 !important;
  text-align: center;
}

.auth-switch a {
  color: #087f5b;
}
</style>
