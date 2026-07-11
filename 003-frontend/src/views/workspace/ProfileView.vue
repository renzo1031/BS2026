<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { VolunteerView } from '@/types/models'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const router = useRouter()
const auth = useAuthStore()
const { user } = storeToRefs(auth)
const isVolunteer = computed(() => user.value?.roleCode === 'VOLUNTEER')
const loading = shallowRef(false)
const saving = shallowRef(false)
const profile = shallowRef<VolunteerView | null>(null)
const form = reactive({ realName: '', phone: '', serviceRegion: '', skills: '', availableTime: '', introduction: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadProfile() {
  if (!isVolunteer.value) return
  loading.value = true
  try {
    profile.value = await api.get<VolunteerView>('/volunteers/me')
    Object.assign(form, {
      realName: profile.value.realName ?? '', phone: profile.value.phone ?? '',
      serviceRegion: profile.value.serviceRegion ?? '', skills: profile.value.skills ?? '',
      availableTime: profile.value.availableTime ?? '', introduction: profile.value.introduction ?? '',
    })
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    profile.value = await api.put<VolunteerView>('/volunteers/me', form)
    message.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function submitCertification() {
  await api.post<void>('/volunteers/me/submit')
  message.success('认证资料已提交审核')
  await loadProfile()
}

async function changePassword() {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.error('两次输入的新密码不一致')
    return
  }
  await api.post<void>('/auth/change-password', passwordForm)
  message.success('密码已修改，请重新登录')
  auth.clear()
  await router.replace('/login')
}

onMounted(loadProfile)
</script>

<template>
  <div class="page-stack">
    <PageHeader title="个人中心" subtitle="维护个人资料和登录密码。" />
    <section v-if="isVolunteer" class="profile-section surface">
      <div class="section-title">
        <div><h2>志愿者认证</h2><p>身份资料仅供管理员审核和服务匹配使用。</p></div>
        <StatusTag v-if="profile" :status="profile.certificationStatus" />
      </div>
      <a-alert v-if="profile?.certificationStatus === 'REJECTED' && profile.rejectionReason" type="error" show-icon :message="profile.rejectionReason" />
      <a-spin :spinning="loading">
        <a-form :model="form" layout="vertical" class="profile-form" @finish="saveProfile">
          <a-form-item label="真实姓名" name="realName" :rules="[{ required: true, message: '请输入真实姓名' }]">
            <a-input v-model:value="form.realName" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" />
          </a-form-item>
          <a-form-item label="联系电话" name="phone" :rules="[{ required: true, pattern: /^1\d{10}$/, message: '请输入正确手机号' }]">
            <a-input v-model:value="form.phone" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" autocomplete="tel" />
          </a-form-item>
          <a-form-item label="服务区域" name="serviceRegion" :rules="[{ required: true, message: '请输入服务区域' }]">
            <a-input v-model:value="form.serviceRegion" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" />
          </a-form-item>
          <a-form-item label="服务技能" name="skills" :rules="[{ required: true, message: '请填写服务技能' }]">
            <a-input v-model:value="form.skills" :maxlength="500" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" />
          </a-form-item>
          <a-form-item label="可服务时间" name="availableTime" :rules="[{ required: true, message: '请填写可服务时间' }]">
            <a-input v-model:value="form.availableTime" :maxlength="300" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" />
          </a-form-item>
          <a-form-item class="wide" label="个人介绍" name="introduction" :rules="[{ required: true, message: '请填写个人介绍' }]">
            <a-textarea v-model:value="form.introduction" :rows="4" :maxlength="1000" show-count :disabled="profile?.certificationStatus === 'PENDING_REVIEW'" />
          </a-form-item>
          <div class="wide form-actions">
            <a-button type="primary" html-type="submit" :loading="saving" :disabled="profile?.certificationStatus === 'PENDING_REVIEW'">保存资料</a-button>
            <a-button
              v-if="profile && ['UNVERIFIED','REJECTED'].includes(profile.certificationStatus)"
              @click="submitCertification"
            >提交审核</a-button>
          </div>
        </a-form>
      </a-spin>
    </section>
    <section class="password-section surface">
      <div class="section-title"><div><h2>修改密码</h2><p>修改后所有登录会话会立即失效。</p></div></div>
      <a-form :model="passwordForm" layout="vertical" class="password-form" @finish="changePassword">
        <a-form-item label="原密码" name="oldPassword" :rules="[{ required: true, message: '请输入原密码' }]">
          <a-input-password v-model:value="passwordForm.oldPassword" autocomplete="current-password" />
        </a-form-item>
        <a-form-item label="新密码" name="newPassword" :rules="[{ required: true, min: 8, pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '至少8位并同时包含字母和数字' }]">
          <a-input-password v-model:value="passwordForm.newPassword" autocomplete="new-password" />
        </a-form-item>
        <a-form-item label="确认新密码" name="confirmPassword" :rules="[{ required: true, message: '请再次输入新密码' }]">
          <a-input-password v-model:value="passwordForm.confirmPassword" autocomplete="new-password" />
        </a-form-item>
        <a-button type="primary" html-type="submit">修改密码</a-button>
      </a-form>
    </section>
  </div>
</template>

<style scoped>
.profile-section,
.password-section {
  padding: 22px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.section-title h2 {
  margin: 0;
  font-size: 18px;
}

.section-title p {
  margin: 5px 0 0;
  color: #68746e;
}

.profile-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
  margin-top: 18px;
}

.wide {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.password-form {
  max-width: 480px;
}

@media (max-width: 680px) {
  .profile-form {
    grid-template-columns: 1fr;
  }

  .wide {
    grid-column: auto;
  }
}
</style>
