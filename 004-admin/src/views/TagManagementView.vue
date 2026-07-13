<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import AsyncState from '@/components/common/AsyncState.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { errorMessage, request } from '@/api/http'
import { usePagedRequest } from '@/composables/usePagedRequest'
import type { CampusView, PageResult, TagView } from '@/types/api'

interface CampusOption {
  id: string
  name: string
  code: string
  identityLabel: string
}

const filters = reactive<{ campusId: string; keyword: string; status: '' | TagView['status'] }>({
  campusId: '',
  keyword: '',
  status: '',
})
const campuses = shallowRef<CampusOption[]>([])
const editorOpen = shallowRef(false)
const saving = shallowRef(false)
const editingId = shallowRef('')
const editor = reactive<{ campusId: string; name: string; status: TagView['status'] }>({
  campusId: '',
  name: '',
  status: 'ACTIVE',
})

const pager = usePagedRequest<TagView>((page, size) =>
  request<PageResult<TagView>>({ url: '/admin/tags', params: query(page, size) }),
)

function query(page: number, size: number) {
  const params: Record<string, string | number> = { page, size }
  if (filters.campusId) params.campusId = filters.campusId
  if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
  if (filters.status) params.status = filters.status
  return params
}

function search() {
  pager.changePage(1)
}

function reset() {
  filters.campusId = ''
  filters.keyword = ''
  filters.status = ''
  search()
}

function create() {
  editingId.value = ''
  editor.campusId = filters.campusId || campuses.value[0]?.id || ''
  editor.name = ''
  editor.status = 'ACTIVE'
  editorOpen.value = true
}

function edit(value: unknown) {
  const row = value as TagView
  editingId.value = row.id
  editor.campusId = row.campusId
  editor.name = row.name
  editor.status = row.status
  editorOpen.value = true
}

async function save() {
  if (!editingId.value && !editor.campusId) {
    ElMessage.warning('请选择校园')
    return
  }
  saving.value = true
  try {
    await request<TagView>({
      method: editingId.value ? 'PUT' : 'POST',
      url: editingId.value ? `/admin/tags/${editingId.value}` : '/admin/tags',
      data: editingId.value
        ? { name: editor.name.trim(), status: editor.status }
        : { campusId: editor.campusId, name: editor.name.trim(), status: editor.status },
    })
    ElMessage.success(editingId.value ? '标签已更新' : '标签已创建')
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

function formatDate(value?: string) {
  return value ? new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-'
}

onMounted(async () => {
  await Promise.all([loadCampuses(), pager.load()])
})
</script>

<template>
  <div class="page">
    <PageHeader title="推荐标签" description="维护校园活动常用标签，学生发布自定义场景时仍可提交新标签。">
      <template #actions><ElButton type="primary" :icon="Plus" @click="create">新增标签</ElButton></template>
    </PageHeader>

    <section class="page-section workbench-section">
      <form class="filters workbench-toolbar" @submit.prevent="search">
        <ElSelect v-model="filters.campusId" clearable filterable placeholder="校园">
          <ElOption v-for="campus in campuses" :key="campus.id" :label="campus.name" :value="campus.id" />
        </ElSelect>
        <ElInput v-model="filters.keyword" clearable placeholder="标签名称" @keyup.enter="search" />
        <ElSelect v-model="filters.status" clearable placeholder="状态">
          <ElOption label="启用" value="ACTIVE" />
          <ElOption label="停用" value="INACTIVE" />
        </ElSelect>
        <ElButton native-type="submit" type="primary" :icon="Search">查询</ElButton>
        <ElButton @click="reset">重置</ElButton>
      </form>

      <AsyncState :loading="pager.loading.value" :error="pager.error.value" :empty="pager.empty.value" empty-text="暂无标签数据" @retry="pager.load">
        <div class="table-wrap">
          <ElTable :data="pager.records.value" row-key="id">
            <ElTableColumn label="校园" min-width="160">
              <template #default="{ row }">{{ row.campusName || row.campusId }}</template>
            </ElTableColumn>
            <ElTableColumn label="标签" prop="name" min-width="140" />
            <ElTableColumn label="归一化值" prop="normalizedName" min-width="160" />
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

    <ElDialog v-model="editorOpen" :title="editingId ? '编辑标签' : '新增标签'" width="min(520px, 94vw)" append-to-body destroy-on-close>
      <ElForm label-position="top">
        <ElFormItem label="校园">
          <ElSelect v-model="editor.campusId" :disabled="Boolean(editingId)" filterable placeholder="请选择校园">
            <ElOption v-for="campus in campuses" :key="campus.id" :label="campus.name" :value="campus.id" />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="标签名称"><ElInput v-model="editor.name" maxlength="12" show-word-limit /></ElFormItem>
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
.filters { display: grid; grid-template-columns: minmax(150px, 1fr) minmax(180px, 1.2fr) 140px auto auto; gap: 10px; }
.pagination { justify-content: flex-end; margin-top: 18px; }
@media (max-width: 840px) { .filters { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .filters { grid-template-columns: 1fr; } }
</style>
