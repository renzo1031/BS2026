<template>
  <section class="page">
    <div class="toolbar">
      <h1 class="page-title">用户管理</h1>
      <n-space class="user-tools">
        <n-form inline @submit.prevent="search">
          <n-input v-model:value="keyword" clearable placeholder="搜索姓名、用户名或学号" style="width: min(260px, 100%)" />
          <n-button attr-type="submit">搜索</n-button>
        </n-form>
        <n-button type="primary" @click="openCreate">创建部门人员</n-button>
      </n-space>
    </div>

    <n-result v-if="errorMessage" status="error" title="用户列表加载失败" :description="errorMessage">
      <template #footer><n-button @click="loadUsers">重试</n-button></template>
    </n-result>
    <template v-else>
      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="false"
        :scroll-x="1120"
        :row-key="(row) => row.id"
      >
        <template #empty><n-empty description="暂无用户" /></template>
      </n-data-table>
      <n-pagination
        v-if="rows.length || pageCount > 1"
        v-model:page="page"
        :page-count="pageCount"
        class="pager"
        @update:page="loadUsers"
      />
    </template>

    <n-modal v-model:show="showCreate" preset="card" title="创建部门人员" style="width: min(620px, calc(100vw - 32px))">
      <n-form ref="staffFormRef" :model="staffForm" :rules="staffRules" label-placement="top" @submit.prevent="createStaff">
        <div class="form-grid">
          <n-form-item label="用户名" path="username">
            <n-input v-model:value="staffForm.username" autocomplete="username" />
          </n-form-item>
          <n-form-item label="姓名" path="realName"><n-input v-model:value="staffForm.realName" autocomplete="name" /></n-form-item>
          <n-form-item label="所属部门" path="departmentId">
            <n-select v-model:value="staffForm.departmentId" :options="departmentOptions" placeholder="请选择部门" />
          </n-form-item>
          <n-form-item label="手机号" path="phone"><n-input v-model:value="staffForm.phone" autocomplete="tel" /></n-form-item>
          <n-form-item label="邮箱" path="email"><n-input v-model:value="staffForm.email" autocomplete="email" /></n-form-item>
          <span aria-hidden="true"></span>
          <n-form-item label="初始密码" path="password">
            <n-input v-model:value="staffForm.password" type="password" show-password-on="click" autocomplete="new-password" />
          </n-form-item>
          <n-form-item label="确认密码" path="confirmPassword">
            <n-input v-model:value="staffForm.confirmPassword" type="password" show-password-on="click" autocomplete="new-password" />
          </n-form-item>
        </div>
        <div class="modal-actions">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button attr-type="submit" type="primary" :loading="creating">创建</n-button>
        </div>
      </n-form>
    </n-modal>
  </section>
</template>

<script setup>
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  NButton, NDataTable, NEmpty, NForm, NFormItem, NInput, NModal, NPagination,
  NResult, NSelect, NSpace, NTag, useMessage
} from 'naive-ui'
import http from '../api/http'
import { formatDateTime } from '../utils/display'

const message = useMessage()
const rows = ref([])
const departments = ref([])
const loading = ref(false)
const errorMessage = ref('')
const keyword = ref('')
const page = ref(1)
const pageCount = ref(1)
const pendingUserId = ref(null)
const showCreate = ref(false)
const creating = ref(false)
const staffFormRef = ref(null)
const staffForm = reactive({
  username: '', realName: '', departmentId: null, phone: '', email: '', password: '', confirmPassword: ''
})

const departmentOptions = computed(() => departments.value.map((department) => ({
  label: department.name,
  value: department.id
})))

const staffRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
    { min: 3, max: 32, message: '用户名长度为 3 至 32 个字符', trigger: ['input', 'blur'] }
  ],
  realName: { required: true, message: '请输入姓名', trigger: ['input', 'blur'] },
  departmentId: { required: true, type: 'number', message: '请选择所属部门', trigger: ['change', 'blur'] },
  phone: { pattern: /^$|^1\d{10}$/, message: '请输入 11 位手机号', trigger: ['input', 'blur'] },
  email: { type: 'email', message: '请输入有效邮箱', trigger: ['input', 'blur'] },
  password: [
    { required: true, message: '请输入初始密码', trigger: ['input', 'blur'] },
    { min: 6, message: '密码至少 6 个字符', trigger: ['input', 'blur'] }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['input', 'blur'] },
    { validator: (_, value) => value === staffForm.password, message: '两次输入的密码不一致', trigger: ['input', 'blur'] }
  ]
}

const columns = [
  { title: '用户名', key: 'username', width: 140 },
  { title: '姓名', key: 'realName', width: 120 },
  { title: '角色', key: 'roles', width: 130, render: (row) => roleText(row.roles) },
  { title: '学号', key: 'studentNo', width: 140, render: (row) => row.studentNo || '-' },
  { title: '部门', key: 'departmentName', width: 150, render: (row) => row.departmentName || departmentName(row.departmentId) },
  {
    title: '状态', key: 'status', width: 90,
    render: (row) => h(NTag, { type: isEnabled(row.status) ? 'success' : 'error', bordered: false }, {
      default: () => isEnabled(row.status) ? '已启用' : '已停用'
    })
  },
  { title: '创建时间', key: 'createdAt', width: 170, render: (row) => formatDateTime(row.createdAt) },
  {
    title: '操作', key: 'actions', width: 100, fixed: 'right',
    render: (row) => h(NButton, {
      size: 'small',
      type: isEnabled(row.status) ? 'error' : 'primary',
      secondary: true,
      loading: pendingUserId.value === row.id,
      onClick: () => toggleStatus(row)
    }, { default: () => isEnabled(row.status) ? '停用' : '启用' })
  }
]

onMounted(async () => {
  await Promise.all([loadUsers(), loadDepartments()])
})

function isEnabled(status) {
  return status === 'ENABLED' || status === true || status === 1
}

function roleText(roles) {
  if (!Array.isArray(roles)) return '-'
  return roles.map((role) => ({ STUDENT: '学生', STAFF: '部门人员', ADMIN: '管理员' }[role] || role)).join('、')
}

function departmentName(id) {
  return departments.value.find((department) => department.id === id)?.name || '-'
}

function search() {
  page.value = 1
  loadUsers()
}

async function loadUsers() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await http.get('/admin/users', {
      params: { page: page.value, size: 10, keyword: keyword.value.trim() || undefined }
    })
    rows.value = result?.records || []
    const size = result?.size || 10
    pageCount.value = Math.max(1, Math.ceil((result?.total || 0) / size))
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  try {
    departments.value = await http.get('/admin/departments') || []
  } catch (error) {
    message.error(`部门加载失败：${error.message}`)
  }
}

function openCreate() {
  Object.assign(staffForm, {
    username: '', realName: '', departmentId: null, phone: '', email: '', password: '', confirmPassword: ''
  })
  staffFormRef.value?.restoreValidation()
  showCreate.value = true
}

async function createStaff() {
  try {
    await staffFormRef.value?.validate()
  } catch {
    return
  }
  creating.value = true
  try {
    await http.post('/admin/staff', {
      username: staffForm.username.trim(),
      realName: staffForm.realName.trim(),
      departmentId: staffForm.departmentId,
      phone: staffForm.phone.trim() || null,
      email: staffForm.email.trim() || null,
      password: staffForm.password
    })
    message.success('部门人员已创建')
    showCreate.value = false
    page.value = 1
    await loadUsers()
  } catch (error) {
    message.error(error.message)
  } finally {
    creating.value = false
  }
}

async function toggleStatus(row) {
  const status = isEnabled(row.status) ? 'DISABLED' : 'ENABLED'
  pendingUserId.value = row.id
  try {
    await http.patch(`/admin/users/${row.id}/status`, { status })
    row.status = status
    message.success(status === 'ENABLED' ? '账号已启用' : '账号已停用')
  } catch (error) {
    message.error(error.message)
  } finally {
    pendingUserId.value = null
  }
}
</script>

<style scoped>
.user-tools {
  align-items: center;
}

.user-tools :deep(.n-form) {
  display: flex;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

@media (max-width: 680px) {
  .user-tools,
  .user-tools :deep(.n-form) {
    width: 100%;
  }

  .user-tools {
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
