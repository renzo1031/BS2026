<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = shallowRef(false)
const form = reactive({ username: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      && !route.query.redirect.startsWith('//') ? route.query.redirect : '/app/dashboard'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-panel">
    <h1>登录平台</h1>
    <p>进入与你的角色和权限对应的工作空间。</p>
    <a-form :model="form" layout="vertical" @finish="submit">
      <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
        <a-input v-model:value="form.username" size="large" autocomplete="username" placeholder="用户名">
          <template #prefix><UserOutlined /></template>
        </a-input>
      </a-form-item>
      <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
        <a-input-password v-model:value="form.password" size="large" autocomplete="current-password" placeholder="密码">
          <template #prefix><LockOutlined /></template>
        </a-input-password>
      </a-form-item>
      <a-button type="primary" html-type="submit" size="large" block :loading="loading">登录</a-button>
    </a-form>
    <p class="auth-switch">还没有志愿者账号？<RouterLink to="/register">立即注册</RouterLink></p>
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
