<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = shallowRef(false)
const form = reactive({ username: 'admin', password: 'password' })

async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page page-shell">
    <div class="auth-card">
      <h1>登录校园失物招领中心</h1>
      <p class="muted">默认测试账号：admin / staff / user，密码均为 password。</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">登录</el-button>
        <el-button @click="router.push('/register')">注册普通用户</el-button>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: 560px;
  display: grid;
  place-items: center;
}

.auth-card {
  width: min(460px, 100%);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 28px;
}

.auth-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
}
</style>
