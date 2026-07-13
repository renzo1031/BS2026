<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { CampusView, PageResult, UserRole, UserView } from '@/types/api'

type ManageableRole = Exclude<UserRole, 'PLATFORM_ADMIN'>

interface CampusOption {
  id: string
  name: string
  code: string
  identityLabel: string
}

const filters = reactive<{ keyword: string; role: '' | UserRole; status: '' | UserView['status']; campusId: string }>({
  keyword: '',
  role: '',
  status: '',
  campusId: '',
})
const campuses = shallowRef<CampusOption[]>([])
const editorOpen = shallowRef(false)
const saving = shallowRef(false)
const editor = reactive<{ id: string; nickname: string; role: ManageableRole; status: UserView['status']; campusId: string }>({
  id: '',
  nickname: '',
  role: 'STUDENT',
  status: 'ACTIVE',
  campusId: '',
})

const pager = usePagedRequest<UserView>((page, size) =>
  request<PageResult<UserView>>({ url: '/admin/users', params: query(page, size) }),
)

function query(page: number, size: number) {
  const params: Record<string, string | number> = { page, size }
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.role) params.role = filters.role
  if (filters.status) params.status = filters.status
  if (filters.campusId) params.campusId = filters.campusId
  return params
}

function search() {
  pager.changePage(1)
}

function reset() {
  filters.keyword = ''
  filters.role = ''
  filters.status = ''
  filters.campusId = ''
  search()
}

function edit(value: unknown) {
  const row = value as UserView
  if (row.role === 'PLATFORM_ADMIN') return
  editor.id = row.id
  editor.nickname = row.nickname
  editor.role = row.role as ManageableRole
  editor.status = row.status
  editor.campusId = row.campusId || ''
  editorOpen.value = true
}

async function save() {
  if (editor.role === 'CAMPUS_REVIEWER' && !editor.campusId) {
    ElMessage.warning('校园审核员必须绑定校园')
    return
  }
  saving.value = true
  try {
    await request<UserView>({
      method: 'PUT',
      url: `/admin/users/${editor.id}`,
      data: {
        nickname: editor.nickname.trim(),
        role: editor.role,
        status: editor.status,
        campusId: editor.campusId || undefined,
      },
    })
    ElMessage.success('用户配置已更新')
    editorOpen.value = false
    await pager.load()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  } finally {
    saving.value = false
  }
}

async function loadCampuses() {
  campuses.value = (await request<PageResult<CampusView>>({ url: '/admin/campuses', params: { page: 1, size: 50 } })).records
}

onMounted(async () => {
  await Promise.all([loadCampuses(), pager.load()])
})
</script>

<template>
  <div class="page">
    <PageHeader title="用户与审核员" description="平台管理员在这里调整学生账号状态，并把已认证的校园人员配置为审核员。">
      <template #actions><ElButton :icon="Refresh" :loading="pager.loading.value" @click="pager.load">刷新</ElButton></template>
    </PageHeader>

    <section class="page-section workbench-section">
      <form class="filters workbench-toolbar" @submit.prevent="search">
        <ElInput v-model="filters.keyword" clearable placeholder="昵称、用户名或 OpenID" @keyup.enter="search" />
        <ElSelect v-model="filters.role" clearable placeholder="角色">
          <ElOption label="学生" value="STUDENT" />
          <ElOption label="校园审核员" value="CAMPUS_REVIEWER" />
          <ElOption label="平台管理员" value="PLATFORM_ADMIN" />
        </ElSelect>
        <ElSelect v-model="filters.status" clearable placeholder="账号状态">
          <ElOption label="正常" value="ACTIVE" />
          <ElOption label="受限" value="LIMITED" />
          <ElOption label="暂停" value="SUSPENDED" />
          <ElOption label="关闭" value="CLOSED" />
        </ElSelect>
        <ElSelect v-model="filters.campusId" clearable filterable placeholder="校园">
          <ElOption v-for="campus in campuses" :key="campus.id" :label="campus.name" :value="campus.id" />
        </ElSelect>
        <ElButton native-type="submit" type="primary" :icon="Search">查询</ElButton>
        <ElButton @click="reset">重置</ElButton>
      </form>

      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="暂无匹配用户" @retry="pager.load">
        <div class="table-wrap">
          <ElTable :data="pager.records.value" row-key="id">
            <ElTableColumn label="用户" min-width="220">
              <template #default="{ row }">
                <span class="stacked">
                  <strong>{{ row.nickname }}</strong>
                  <span>ID {{ row.id }}</span>
                </span>
              </template>
            </ElTableColumn>
            <ElTableColumn label="角色" width="130">
              <template #default="{ row }">{{ row.role === 'PLATFORM_ADMIN' ? '平台管理员' : row.role === 'CAMPUS_REVIEWER' ? '校园审核员' : '学生' }}</template>
            </ElTableColumn>
            <ElTableColumn label="校园" min-width="150">
              <template #default="{ row }">{{ row.campusName || '未绑定' }}</template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="120">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </ElTableColumn>
            <ElTableColumn label="认证" width="120">
              <template #default="{ row }"><StatusTag :status="row.verificationStatus" /></template>
            </ElTableColumn>
            <ElTableColumn label="账号标识" min-width="220">
              <template #default="{ row }">{{ row.username || row.wechatOpenid || '未设置' }}</template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <ElButton link type="primary" :icon="Edit" :disabled="row.role === 'PLATFORM_ADMIN'" @click="edit(row)">配置</ElButton>
              </template>
            </ElTableColumn>
          </ElTable>
        </div>
        <ElPagination
          class="pagination"
          :current-page="pager.page.value"
          :page-size="pager.size.value"
          :total="pager.total.value"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="pager.changePage"
          @size-change="pager.changeSize"
        />
      </AsyncState>
    </section>

    <ElDialog v-model="editorOpen" title="配置用户与审核员" width="min(520px, 94vw)" append-to-body destroy-on-close>
      <ElForm label-position="top">
        <ElFormItem label="昵称"><ElInput v-model="editor.nickname" maxlength="40" show-word-limit /></ElFormItem>
        <ElFormItem label="角色">
          <ElSelect v-model="editor.role">
            <ElOption label="学生" value="STUDENT" />
            <ElOption label="校园审核员" value="CAMPUS_REVIEWER" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="校园">
          <ElSelect v-model="editor.campusId" clearable filterable placeholder="学生可为空，审核员必选">
            <ElOption v-for="campus in campuses" :key="campus.id" :label="campus.name" :value="campus.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="editor.status">
            <ElOption label="正常" value="ACTIVE" />
            <ElOption label="受限" value="LIMITED" />
            <ElOption label="暂停" value="SUSPENDED" />
            <ElOption label="关闭" value="CLOSED" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <ElButton @click="editorOpen = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="save">保存</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<style scoped>
.filters { display: grid; grid-template-columns: minmax(180px, 1.6fr) repeat(3, minmax(130px, 1fr)) auto auto; gap: 10px; }
.pagination { justify-content: flex-end; margin-top: 18px; }
.stacked { display: grid; gap: 4px; }
.stacked span { color: var(--color-muted); font-size: 12px; }
@media (max-width: 980px) { .filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .filters { grid-template-columns: 1fr; } }
</style>
