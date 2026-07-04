<script setup lang="ts">
import { computed } from 'vue'

interface PasswordFormModel {
  oldPassword: string
  newPassword: string
}

const model = defineModel<PasswordFormModel>({ required: true })

defineProps<{
  changing?: boolean
}>()

const emit = defineEmits<{
  changePassword: []
}>()

const canSubmit = computed(() => Boolean(model.value.oldPassword) && model.value.newPassword.length >= 6)
</script>

<template>
  <el-form class="profile-panel security-panel" label-position="top">
    <div class="panel-head">
      <div>
        <h2 class="panel-title">账号安全</h2>
        <p class="panel-copy">修改密码会立即影响下次登录，建议定期更新。</p>
      </div>
      <el-tag type="warning" effect="plain">密码保护</el-tag>
    </div>

    <el-form-item label="原密码">
      <el-input
        v-model="model.oldPassword"
        type="password"
        autocomplete="current-password"
        show-password
        placeholder="请输入当前密码"
      />
    </el-form-item>
    <el-form-item label="新密码">
      <el-input
        v-model="model.newPassword"
        type="password"
        autocomplete="new-password"
        show-password
        placeholder="至少 6 位"
      />
    </el-form-item>

    <div class="security-rules">
      <strong>密码要求</strong>
      <span>至少 6 位字符</span>
      <span>不要和其他平台复用相同密码</span>
      <span>修改后请妥善保存新密码</span>
    </div>

    <el-button class="security-action" type="primary" :disabled="!canSubmit" :loading="changing" @click="emit('changePassword')">
      修改密码
    </el-button>
  </el-form>
</template>

<style scoped>
.profile-panel {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-title {
  margin: 0;
  font-size: 20px;
}

.panel-copy {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.security-rules {
  display: grid;
  gap: 8px;
  margin: 4px 0 16px;
  padding: 14px;
  border-radius: 8px;
  background: #f6f9fc;
  color: #52677c;
  font-size: 13px;
}

.security-rules strong {
  color: #132f4b;
}

.security-action {
  width: 100%;
}

@media (max-width: 720px) {
  .panel-head {
    flex-direction: column;
  }
}
</style>
