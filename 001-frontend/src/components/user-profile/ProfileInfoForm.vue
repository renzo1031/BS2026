<script setup lang="ts">
import { computed } from 'vue'

interface ProfileFormModel {
  realName: string
  phone: string
  studentNo: string
  email: string
}

const model = defineModel<ProfileFormModel>({ required: true })

const props = defineProps<{
  username?: string
  roles: string[]
  saving?: boolean
}>()

const emit = defineEmits<{
  save: []
}>()

const roleLabelMap: Record<string, string> = {
  ADMIN: '系统管理员',
  STAFF: '物品保管员',
  USER: '普通用户'
}

const roleText = computed(() => props.roles.map((role) => roleLabelMap[role] || role).join('、') || '普通用户')
</script>

<template>
  <el-form class="profile-panel info-form" label-position="top">
    <div class="panel-head">
      <div>
        <h2 class="panel-title">基本资料</h2>
        <p class="panel-copy">这些信息会用于发布登记、认领核验和线下交接联系。</p>
      </div>
      <el-tag effect="plain">可编辑</el-tag>
    </div>

    <div class="form-grid">
      <el-form-item label="用户名">
        <el-input :model-value="username" disabled />
      </el-form-item>
      <el-form-item label="当前角色">
        <el-input :model-value="roleText" disabled />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="model.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="model.phone" placeholder="用于后台联系和交接确认" />
      </el-form-item>
      <el-form-item label="学号/工号">
        <el-input v-model="model.studentNo" placeholder="学生或教职工编号" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="model.email" placeholder="用于接收补充通知" />
      </el-form-item>
    </div>

    <div class="form-actions">
      <el-button type="primary" :loading="saving" @click="emit('save')">保存资料</el-button>
    </div>
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }
}
</style>
