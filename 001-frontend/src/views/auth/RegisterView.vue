<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(false)
const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  studentNo: '',
  email: ''
})

async function submit() {
  loading.value = true
  try {
    await auth.register(form)
    ElMessage.success('注册成功')
    router.push('/user')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-page page-shell">
    <div class="auth-card">
      <h1>注册普通用户</h1>
      <el-form label-position="top" @submit.prevent>
        <div class="form-grid">
          <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="学号/工号"><el-input v-model="form.studentNo" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        </div>
        <el-button type="primary" :loading="loading" @click="submit">注册并登录</el-button>
        <el-button @click="router.push('/login')">已有账号</el-button>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.auth-page {
  min-height: 600px;
  display: grid;
  place-items: center;
}

.auth-card {
  width: min(720px, 100%);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 28px;
}

.auth-card h1 {
  margin: 0 0 18px;
  font-size: 24px;
}
</style>
