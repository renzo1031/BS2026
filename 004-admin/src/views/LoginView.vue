<script setup lang="ts">
import { reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { errorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = shallowRef<FormInstance>()
const submitting = shallowRef(false)
const form = reactive({ username: '', password: '' })
const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' },
  ],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : '/'
    await router.replace(redirect)
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main id="main-content" class="login-page">
    <section class="context" aria-labelledby="platform-title">
      <div class="context__eyebrow">CAMPUS BUDDY / TRUST &amp; SAFETY</div>
      <h1 id="platform-title" class="context__title">让每一次相遇，<br>先经过认真守护。</h1>
      <p class="context__lead">
        校园搭子审核与治理中心，用于校园身份、学生自定义活动、举报申诉和文件内容的合规处理。
      </p>
      <dl class="principles">
        <div><dt>01</dt><dd>只处理授权校园范围</dd></div>
        <div><dt>02</dt><dd>关键决策留痕且可申诉</dd></div>
        <div><dt>03</dt><dd>证明与证据保持私有</dd></div>
      </dl>
    </section>

    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-panel__inner">
        <span class="login-panel__mark" aria-hidden="true">伴</span>
        <h2 id="login-title" class="login-panel__title">管理账号登录</h2>
        <p class="login-panel__hint">仅限校园审核员与平台管理员使用</p>
        <ElForm ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <ElFormItem label="账号" prop="username">
            <ElInput v-model="form.username" autocomplete="username" placeholder="请输入账号" size="large" />
          </ElFormItem>
          <ElFormItem label="密码" prop="password">
            <ElInput
              v-model="form.password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
              size="large"
              type="password"
              @keyup.enter="submit"
            />
          </ElFormItem>
          <ElButton class="login-panel__submit" type="primary" size="large" :loading="submitting" @click="submit">
            登录治理中心
          </ElButton>
        </ElForm>
        <p class="login-panel__security">请勿共享管理账号；处理敏感材料后请及时安全退出。</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { display: grid; min-height: 100vh; grid-template-columns: minmax(0, 1.16fr) minmax(420px, 0.84fr); background: #102b23; }
.context { position: relative; display: flex; min-height: 100vh; flex-direction: column; justify-content: center; padding: clamp(42px, 7vw, 112px); overflow: hidden; color: #f2f7f4; isolation: isolate; }
.context::before { position: absolute; inset: 0; z-index: -1; background-image: linear-gradient(rgb(255 255 255 / 4%) 1px, transparent 1px), linear-gradient(90deg, rgb(255 255 255 / 4%) 1px, transparent 1px); background-size: 48px 48px; mask-image: linear-gradient(135deg, #000, transparent 70%); content: ''; }
.context::after { position: absolute; right: 8%; bottom: 12%; z-index: -1; width: min(26vw, 350px); height: min(26vw, 350px); border: 1px solid rgb(232 215 163 / 25%); border-radius: 50%; box-shadow: 0 0 0 24px rgb(232 215 163 / 3%), 0 0 0 48px rgb(232 215 163 / 2%); content: ''; }
.context__eyebrow { position: relative; color: #b7c9c2; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 10px; letter-spacing: 0.16em; }
.context__title { position: relative; max-width: 720px; margin: 24px 0 22px; font-family: "Noto Serif SC", "Songti SC", serif; font-size: clamp(38px, 4.6vw, 66px); font-weight: 600; line-height: 1.2; }
.context__lead { position: relative; max-width: 590px; margin: 0; color: #bfd0ca; font-size: 15px; line-height: 1.85; }
.principles { position: relative; display: grid; max-width: 720px; grid-template-columns: repeat(3, 1fr); gap: 1px; margin: 52px 0 0; background: rgb(255 255 255 / 14%); }
.principles div { min-height: 100px; padding: 16px; background: rgb(16 43 35 / 92%); }
.principles dt { color: #ecd58f; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 10px; }
.principles dd { margin: 10px 0 0; color: #e6efeb; font-size: 12px; line-height: 1.55; }
.login-panel { display: grid; min-height: 100vh; place-items: center; padding: 42px; background: #f4f5f2; border-left: 1px solid rgb(255 255 255 / 10%); }
.login-panel__inner { width: min(100%, 390px); padding: 28px; background: #fff; border: 1px solid #dfe6e2; border-radius: 6px; box-shadow: 0 22px 55px rgb(23 42 35 / 10%); }
.login-panel__mark { display: grid; width: 42px; height: 42px; place-items: center; color: #15372d; font-size: 18px; font-weight: 800; background: #e6d59c; border-radius: 10px 3px; }
.login-panel__title { margin: 24px 0 8px; color: #17211d; font-size: 26px; font-weight: 700; }
.login-panel__hint { margin: 0 0 26px; color: var(--color-muted); font-size: 13px; }
.login-panel__submit { width: 100%; margin-top: 8px; }
.login-panel__security { margin: 20px 0 0; color: #87948d; font-size: 11px; line-height: 1.6; }
:deep(.el-form-item__label) { color: #415249; font-size: 12px; font-weight: 600; }
:deep(.el-input__wrapper) { min-height: 42px; box-shadow: 0 0 0 1px #dce5df inset; }
:deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--color-primary) inset, 0 0 0 3px rgb(25 123 89 / 10%); }
@media (max-width: 860px) {
  .login-page { grid-template-columns: 1fr; }
  .context { min-height: auto; padding: 48px 28px 40px; }
  .context__title { margin-top: 18px; font-size: 38px; }
  .principles { display: none; }
  .login-panel { min-height: auto; padding: 38px 18px 68px; border-left: 0; }
  .login-panel__inner { padding: 24px; }
}
</style>
