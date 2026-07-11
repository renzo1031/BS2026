<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import type { PermissionView, RoleView } from '@/types/models'
import PageHeader from '@/components/common/PageHeader.vue'

const loading = shallowRef(true)
const roles = shallowRef<RoleView[]>([])
const permissions = shallowRef<PermissionView[]>([])
const selections = reactive<Record<number, number[]>>({})

const groupedPermissions = computed(() => {
  const groups = new Map<string, PermissionView[]>()
  permissions.value.forEach((permission) => {
    groups.set(permission.module, [...(groups.get(permission.module) ?? []), permission])
  })
  return [...groups.entries()]
})

async function load() {
  loading.value = true
  try {
    const [roleData, permissionData] = await Promise.all([
      api.get<RoleView[]>('/admin/roles'), api.get<PermissionView[]>('/admin/permissions'),
    ])
    roles.value = roleData
    permissions.value = permissionData
    roleData.forEach(role => { selections[role.id] = [...role.permissionIds] })
  } finally {
    loading.value = false
  }
}

async function save(role: RoleView) {
  await api.put<void>(`/admin/roles/${role.id}/permissions`, { permissionIds: selections[role.id] })
  message.success(`${role.name}权限已更新`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <PageHeader title="角色权限" subtitle="系统管理员权限为安全基线，其余角色按业务职责配置。" />
    <a-spin :spinning="loading">
      <section v-for="role in roles" :key="role.id" class="role-section surface">
        <header>
          <div><h2>{{ role.name }}</h2><p>{{ role.code }} · {{ role.dataScope }}</p></div>
          <a-button v-if="role.code !== 'SYS_ADMIN'" type="primary" @click="save(role)">保存权限</a-button>
          <a-tag v-else color="green">安全基线</a-tag>
        </header>
        <div class="permission-groups">
          <div v-for="[module, items] in groupedPermissions" :key="module" class="permission-group">
            <h3>{{ module }}</h3>
            <a-checkbox-group
              v-model:value="selections[role.id]"
              :disabled="role.code === 'SYS_ADMIN'"
              :options="items.map(item => ({ value: item.id, label: item.name }))"
            />
          </div>
        </div>
      </section>
    </a-spin>
  </div>
</template>

<style scoped>
.role-section {
  padding: 20px;
}

.role-section > header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8e5;
}

.role-section h2,
.role-section h3 {
  margin: 0;
}

.role-section header p {
  margin: 5px 0 0;
  color: #6a7670;
}

.permission-groups {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  padding-top: 20px;
}

.permission-group h3 {
  margin-bottom: 12px;
  color: #30453e;
  font-size: 14px;
  text-transform: uppercase;
}

.permission-group :deep(.ant-checkbox-group) {
  display: grid;
  gap: 9px;
}

@media (max-width: 820px) {
  .permission-groups {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 520px) {
  .permission-groups {
    grid-template-columns: 1fr;
  }
}
</style>
