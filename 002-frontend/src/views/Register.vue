<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="register-title">
      <h1 id="register-title">学生注册</h1>
      <n-form ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
        <div class="register-grid">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="form.username" autocomplete="username" placeholder="3 至 32 个字符" />
          </n-form-item>
          <n-form-item label="姓名" path="realName">
            <n-input v-model:value="form.realName" autocomplete="name" placeholder="与学生名册一致" />
          </n-form-item>
          <n-form-item label="学号" path="studentNo">
            <n-input v-model:value="form.studentNo" placeholder="与学生名册一致" />
          </n-form-item>
          <n-form-item label="学院" path="college"><n-input v-model:value="form.college" /></n-form-item>
          <n-form-item label="专业" path="major"><n-input v-model:value="form.major" /></n-form-item>
          <n-form-item label="手机号" path="phone"><n-input v-model:value="form.phone" autocomplete="tel" /></n-form-item>
          <n-form-item label="邮箱" path="email"><n-input v-model:value="form.email" autocomplete="email" /></n-form-item>
          <span class="grid-spacer" aria-hidden="true"></span>
          <n-form-item label="密码" path="password">
            <n-input v-model:value="form.password" type="password" show-password-on="click" autocomplete="new-password" />
          </n-form-item>
          <n-form-item label="确认密码" path="confirmPassword">
            <n-input v-model:value="form.confirmPassword" type="password" show-password-on="click" autocomplete="new-password" />
          </n-form-item>
        </div>
        <n-space justify="space-between">
          <n-button type="default" @click="router.push('/login')">返回登录</n-button>
          <n-button attr-type="submit" type="primary" :loading="loading">注册</n-button>
        </n-space>
      </n-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, NSpace, useMessage } from 'naive-ui'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '', password: '', confirmPassword: '', realName: '', studentNo: '',
  phone: '', email: '', college: '', major: ''
})
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
    { min: 3, max: 32, message: '用户名长度为 3 至 32 个字符', trigger: ['input', 'blur'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    { min: 6, message: '密码至少 6 个字符', trigger: ['input', 'blur'] }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['input', 'blur'] },
    { validator: (_, value) => value === form.password, message: '两次输入的密码不一致', trigger: ['input', 'blur'] }
  ],
  realName: { required: true, message: '请输入名册中的姓名', trigger: ['input', 'blur'] },
  studentNo: { required: true, message: '请输入学号', trigger: ['input', 'blur'] },
  phone: { pattern: /^$|^1\d{10}$/, message: '请输入 11 位手机号', trigger: ['input', 'blur'] },
  email: { type: 'email', message: '请输入有效邮箱', trigger: ['input', 'blur'] }
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const { confirmPassword, ...payload } = form
    await auth.register(payload)
    message.success('注册成功，请登录')
    await router.replace('/login')
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
  width: min(760px, 100%);
  padding: 28px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 16px 50px rgba(15, 23, 42, 0.12);
}

h1 {
  margin: 0 0 22px;
  font-size: 26px;
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

@media (max-width: 640px) {
  .auth-page {
    align-items: start;
    padding: 16px;
  }

  .auth-panel {
    padding: 20px;
  }

  .register-grid {
    grid-template-columns: 1fr;
  }

  .grid-spacer {
    display: none;
  }
}
</style>
