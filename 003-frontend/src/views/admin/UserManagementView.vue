<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { api } from '@/api/http'
import type { DepartmentView, PageResult, RoleView, UserAdminView } from '@/types/models'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const loading = shallowRef(false)
const users = shallowRef<PageResult<UserAdminView>>({ items: [], total: 0, page: 1, size: 10 })
const departments = shallowRef<DepartmentView[]>([])
const roles = shallowRef<RoleView[]>([])
const keyword = shallowRef('')
const userOpen = shallowRef(false)
const departmentOpen = shallowRef(false)
const userForm = reactive({ username: '', password: '', displayName: '', roleId: undefined as number | undefined, departmentId: undefined as number | undefined })
const departmentForm = reactive({ code: '', name: '' })

async function loadUsers(page = 1) {
  loading.value = true
  try {
    users.value = await api.get<PageResult<UserAdminView>>('/admin/users', {
      page, size: users.value.size, keyword: keyword.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

async function loadLookups() {
  const [departmentData, roleData] = await Promise.all([
    api.get<DepartmentView[]>('/admin/departments'),
    api.get<RoleView[]>('/admin/roles'),
  ])
  departments.value = departmentData
  roles.value = roleData
}

async function createUser() {
  await api.post<void>('/admin/users', userForm)
  message.success('账号已创建')
  userOpen.value = false
  Object.assign(userForm, { username: '', password: '', displayName: '', roleId: undefined, departmentId: undefined })
  await loadUsers(1)
}

async function createDepartment() {
  await api.post<void>('/admin/departments', departmentForm)
  message.success('部门已创建')
  departmentOpen.value = false
  Object.assign(departmentForm, { code: '', name: '' })
  await loadLookups()
}

function toggleStatus(user: UserAdminView) {
  const nextStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  Modal.confirm({
    title: nextStatus === 'DISABLED' ? '确认停用该账号？' : '确认启用该账号？',
    content: nextStatus === 'DISABLED' ? '停用后，该账号的全部登录会话会立即失效。' : user.displayName,
    async onOk() {
      await api.patch<void>(`/admin/users/${user.id}/status`, { status: nextStatus })
      message.success('账号状态已更新')
      await loadUsers(users.value.page)
    },
  })
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadLookups()])
})
</script>

<template>
  <div class="page-stack">
    <PageHeader title="用户与部门" subtitle="内部业务账号由管理员创建，志愿者可从官网自主注册。">
      <template #actions>
        <a-button @click="departmentOpen = true"><PlusOutlined /> 新建部门</a-button>
        <a-button type="primary" @click="userOpen = true"><PlusOutlined /> 新建账号</a-button>
      </template>
    </PageHeader>
    <div class="surface">
      <div class="table-toolbar">
        <div class="toolbar-fields">
          <a-input v-model:value="keyword" allow-clear placeholder="用户名或显示名称" @press-enter="loadUsers(1)" />
          <a-button type="primary" @click="loadUsers(1)">查询</a-button>
        </div>
      </div>
      <a-table :data-source="users.items" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 980 }">
        <a-table-column title="用户名" data-index="username" :width="140" />
        <a-table-column title="显示名称" data-index="displayName" :width="150" />
        <a-table-column title="角色" data-index="roleName" :width="140" />
        <a-table-column title="部门" data-index="departmentName" :width="180"><template #default="{ text }">{{ text || '-' }}</template></a-table-column>
        <a-table-column title="状态" :width="110"><template #default="{ record }"><StatusTag :status="record.status" /></template></a-table-column>
        <a-table-column title="最后登录" data-index="lastLoginAt" :width="180"><template #default="{ text }">{{ text || '从未登录' }}</template></a-table-column>
        <a-table-column title="创建时间" data-index="createdAt" :width="180" />
        <a-table-column title="操作" :width="110" fixed="right"><template #default="{ record }"><a-button type="link" :danger="record.status === 'ACTIVE'" @click="toggleStatus(record)">{{ record.status === 'ACTIVE' ? '停用' : '启用' }}</a-button></template></a-table-column>
      </a-table>
      <a-pagination v-if="users.total > users.size" class="pagination" :current="users.page" :page-size="users.size" :total="users.total" :show-size-changer="false" @change="loadUsers" />
    </div>
    <section class="surface department-section">
      <h2>业务部门</h2>
      <a-table :data-source="departments" row-key="id" :pagination="false">
        <a-table-column title="部门编码" data-index="code" />
        <a-table-column title="部门名称" data-index="name" />
        <a-table-column title="状态"><template #default="{ record }"><a-tag :color="record.enabled ? 'green' : 'red'">{{ record.enabled ? '启用' : '停用' }}</a-tag></template></a-table-column>
      </a-table>
    </section>

    <a-modal v-model:open="userOpen" title="新建账号" @ok="createUser">
      <a-form :model="userForm" layout="vertical">
        <a-form-item label="用户名" required><a-input v-model:value="userForm.username" /></a-form-item>
        <a-form-item label="显示名称" required><a-input v-model:value="userForm.displayName" /></a-form-item>
        <a-form-item label="初始密码" required><a-input-password v-model:value="userForm.password" autocomplete="new-password" /></a-form-item>
        <a-form-item label="角色" required><a-select v-model:value="userForm.roleId" :options="roles.map(role => ({ value: role.id, label: role.name }))" /></a-form-item>
        <a-form-item label="业务部门"><a-select v-model:value="userForm.departmentId" allow-clear :options="departments.map(item => ({ value: item.id, label: item.name }))" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="departmentOpen" title="新建部门" @ok="createDepartment">
      <a-form :model="departmentForm" layout="vertical">
        <a-form-item label="部门编码" required><a-input v-model:value="departmentForm.code" placeholder="例如 SERVICE_CENTER_02" /></a-form-item>
        <a-form-item label="部门名称" required><a-input v-model:value="departmentForm.name" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.pagination {
  padding: 16px;
  text-align: right;
}

.department-section {
  padding: 20px;
}

.department-section h2 {
  margin: 0 0 14px;
  font-size: 18px;
}
</style>
