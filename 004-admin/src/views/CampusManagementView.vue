<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { CampusView, PageResult } from '@/types/api'

const filters = reactive<{ keyword: string; status: '' | CampusView['status'] }>({ keyword: '', status: '' })
const editorOpen = shallowRef(false)
const saving = shallowRef(false)
const editingId = shallowRef('')
const editor = reactive<{ name: string; code: string; status: CampusView['status']; identityLabel: string }>({
  name: '',
  code: '',
  status: 'ACTIVE',
  identityLabel: '学号',
})

const pager = usePagedRequest<CampusView>((page, size) =>
  request<PageResult<CampusView>>({ url: '/admin/campuses', params: query(page, size) }),
)

function query(page: number, size: number) {
  const params: Record<string, string | number> = { page, size }
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.status) params.status = filters.status
  return params
}

function search() {
  pager.changePage(1)
}

function reset() {
  filters.keyword = ''
  filters.status = ''
  search()
}

function create() {
  editingId.value = ''
  editor.name = ''
  editor.code = ''
  editor.status = 'ACTIVE'
  editor.identityLabel = '学号'
  editorOpen.value = true
}

function edit(value: unknown) {
  const row = value as CampusView
  editingId.value = row.id
  editor.name = row.name
  editor.code = row.code
  editor.status = row.status
  editor.identityLabel = row.identityLabel
  editorOpen.value = true
}

async function save() {
  saving.value = true
  try {
    await request<CampusView>({
      method: editingId.value ? 'PUT' : 'POST',
      url: editingId.value ? `/admin/campuses/${editingId.value}` : '/admin/campuses',
      data: { name: editor.name.trim(), code: editor.code.trim(), status: editor.status, identityLabel: editor.identityLabel.trim() },
    })
    ElMessage.success(editingId.value ? '校园信息已更新' : '校园已创建')
    editorOpen.value = false
    await pager.load()
  } catch (cause) {
    ElMessage.error(errorMessage(cause))
  } finally {
    saving.value = false
  }
}

function formatDate(value?: string) {
  return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-'
}

onMounted(pager.load)
</script>

<template>
  <div class="page">
    <PageHeader title="校园管理" description="维护可入驻校园、认证标识名称和启用状态；停用后学生侧不会再展示该校园。">
      <template #actions><ElButton type="primary" :icon="Plus" @click="create">新增校园</ElButton></template>
    </PageHeader>

    <section class="page-section workbench-section">
      <form class="filters workbench-toolbar" @submit.prevent="search">
        <ElInput v-model="filters.keyword" clearable placeholder="校园名称、代码或认证标识" @keyup.enter="search" />
        <ElSelect v-model="filters.status" clearable placeholder="状态">
          <ElOption label="启用" value="ACTIVE" />
          <ElOption label="停用" value="INACTIVE" />
        </ElSelect>
        <ElButton native-type="submit" type="primary" :icon="Search">查询</ElButton>
        <ElButton @click="reset">重置</ElButton>
      </form>

      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="暂无校园数据" @retry="pager.load">
        <div class="table-wrap">
          <ElTable :data="pager.records.value" row-key="id">
            <ElTableColumn label="校园名称" prop="name" min-width="180" />
            <ElTableColumn label="代码" prop="code" width="130" />
            <ElTableColumn label="认证标识" prop="identityLabel" width="130" />
            <ElTableColumn label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </ElTableColumn>
            <ElTableColumn label="创建时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="100" fixed="right">
              <template #default="{ row }"><ElButton link type="primary" :icon="Edit" @click="edit(row)">编辑</ElButton></template>
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

    <ElDialog v-model="editorOpen" :title="editingId ? '编辑校园' : '新增校园'" width="min(520px, 94vw)" append-to-body destroy-on-close>
      <ElForm label-position="top">
        <ElFormItem label="校园名称"><ElInput v-model="editor.name" maxlength="80" show-word-limit /></ElFormItem>
        <ElFormItem label="校园代码"><ElInput v-model="editor.code" maxlength="32" show-word-limit /></ElFormItem>
        <ElFormItem label="认证标识"><ElInput v-model="editor.identityLabel" maxlength="40" show-word-limit /></ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="editor.status">
            <ElOption label="启用" value="ACTIVE" />
            <ElOption label="停用" value="INACTIVE" />
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
.filters { display: grid; grid-template-columns: minmax(220px, 1fr) 160px auto auto; gap: 10px; }
.pagination { justify-content: flex-end; margin-top: 18px; }
@media (max-width: 720px) { .filters { grid-template-columns: 1fr; } }
</style>
