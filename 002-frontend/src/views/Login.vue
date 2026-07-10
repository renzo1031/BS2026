<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="login-title">
      <h1 id="login-title">大学生一体化服务平台</h1>
      <p>统一办理、跟踪和评价校园学生事务。</p>
      <n-form ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" autocomplete="username" placeholder="请输入用户名" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </n-form-item>
        <n-button attr-type="submit" type="primary" block :loading="loading">登录</n-button>
      </n-form>
      <div class="auth-links">
        <router-link to="/register">学生注册</router-link>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
  password: { required: true, message: '请输入密码', trigger: ['input', 'blur'] }
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await auth.login(form)
    message.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : auth.homePath
    await router.replace(redirect)
  } catch (error) {
    message.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #eef3f7;
}

.auth-panel {
  width: min(420px, 100%);
  padding: 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 16px 50px rgba(15, 23, 42, 0.12);
}

h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

p {
  margin: 0 0 24px;
  color: #64748b;
}

.auth-links {
  margin-top: 16px;
  color: #0f766e;
}
</style>
