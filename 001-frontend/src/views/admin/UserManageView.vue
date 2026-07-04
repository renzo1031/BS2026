<script setup lang="ts">
import { onMounted, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/modules'
import type { User } from '../../types'

const loading = shallowRef(false)
const users = ref<User[]>([])

async function load() {
  loading.value = true
  try {
    users.value = (await adminApi.users({ pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

async function toggle(row: User) {
  const status = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  await adminApi.updateUserStatus(row.id, { status })
  ElMessage.success('用户状态已更新')
  await load()
}

async function resetPassword(row: User) {
  let value = ''
  try {
    const result = await ElMessageBox.prompt(`请输入 ${row.realName || row.username} 的新密码`, '重置密码', {
      inputType: 'password',
      inputPlaceholder: '至少 6 位',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputValidator: (value) => {
        if (!value || value.length < 6) {
          return '新密码至少 6 位'
        }
        return true
      }
    })
    value = String(result.value)
  } catch {
    // 用户取消时不提示错误。
    return
  }
  await adminApi.resetUserPassword(row.id, { newPassword: value })
  ElMessage.success('密码已重置')
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    ENABLED: '启用',
    DISABLED: '禁用'
  }
  return status ? labels[status] || status : '-'
}

function statusTagType(status?: string) {
  return status === 'ENABLED' ? 'success' : 'info'
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">用户管理</h1>
    <el-table v-loading="loading" :data="users" row-key="id">
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="150" />
      <el-table-column prop="studentNo" label="学号/工号" width="150" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button link type="primary" @click="toggle(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button>
            <el-button link type="warning" @click="resetPassword(row)">重置密码</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
