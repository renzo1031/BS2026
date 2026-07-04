<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '../../api/modules'
import type { Notice } from '../../types'

interface NoticeForm {
  id?: number
  title: string
  content: string
  noticeType: string
  publishStatus: string
  startTime: string
  endTime: string
  popupEnabled: boolean
}

const loading = shallowRef(false)
const saving = shallowRef(false)
const notices = ref<Notice[]>([])
const form = reactive<NoticeForm>({
  title: '',
  content: '',
  noticeType: 'ANNOUNCEMENT',
  publishStatus: 'PUBLISHED',
  startTime: '',
  endTime: '',
  popupEnabled: false
})

const formTitle = computed(() => (form.id ? '编辑公告' : '发布公告'))
const submitLabel = computed(() => (form.id ? '保存修改' : '发布公告'))

async function load() {
  loading.value = true
  try {
    notices.value = (await adminApi.notices({ pageNum: 1, pageSize: 50 })).records
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.title = ''
  form.content = ''
  form.noticeType = 'ANNOUNCEMENT'
  form.publishStatus = 'PUBLISHED'
  form.startTime = ''
  form.endTime = ''
  form.popupEnabled = false
}

function normalizeDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

function editNotice(row: Notice) {
  form.id = row.id
  form.title = row.title
  form.content = row.content
  form.noticeType = row.noticeType || 'ANNOUNCEMENT'
  form.publishStatus = row.publishStatus || 'PUBLISHED'
  form.startTime = normalizeDate(row.startTime)
  form.endTime = normalizeDate(row.endTime)
  form.popupEnabled = row.popupEnabled === 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  saving.value = true
  try {
    await adminApi.saveNotice({
      title: form.title,
      content: form.content,
      noticeType: form.noticeType,
      publishStatus: form.publishStatus,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
      popupEnabled: form.popupEnabled
    }, form.id)
    ElMessage.success(form.id ? '公告已更新' : '公告已发布')
    resetForm()
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: Notice) {
  const nextStatus = row.publishStatus === 'PUBLISHED' ? 'PAUSED' : 'PUBLISHED'
  await adminApi.saveNotice({
    title: row.title,
    content: row.content,
    noticeType: row.noticeType,
    publishStatus: nextStatus,
    startTime: normalizeDate(row.startTime) || null,
    endTime: normalizeDate(row.endTime) || null,
    popupEnabled: row.popupEnabled === 1
  }, row.id)
  ElMessage.success(nextStatus === 'PUBLISHED' ? '公告已启用' : '公告已暂停')
  await load()
}

async function removeNotice(row: Notice) {
  await ElMessageBox.confirm(`确认删除公告“${row.title}”？删除后前台不再展示。`, '删除公告', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await adminApi.deleteNotice(row.id)
  ElMessage.success('公告已删除')
  if (form.id === row.id) {
    resetForm()
  }
  await load()
}

function activeStatus(row: Notice) {
  if (row.publishStatus !== 'PUBLISHED') return { label: '已暂停', type: 'info' }
  const now = Date.now()
  const start = row.startTime ? Date.parse(row.startTime) : null
  const end = row.endTime ? Date.parse(row.endTime) : null
  if (start && start > now) return { label: '未开始', type: 'warning' }
  if (end && end < now) return { label: '已结束', type: 'info' }
  return { label: '展示中', type: 'success' }
}

onMounted(load)
</script>

<template>
  <section>
    <h1 class="section-title">公告管理</h1>
    <div class="panel notice-form">
      <div class="form-head">
        <div>
          <h2>{{ formTitle }}</h2>
          <p>设置前台展示时间、是否首页弹窗，以及公告启停状态。</p>
        </div>
        <el-button v-if="form.id" @click="resetForm">取消编辑</el-button>
      </div>
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="标题"><el-input v-model="form.title" maxlength="120" show-word-limit /></el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="form.publishStatus">
              <el-option label="发布中" value="PUBLISHED" />
              <el-option label="暂停展示" value="PAUSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="展示开始时间">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填则立即生效"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="展示结束时间">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不填则长期有效"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
        <div class="form-actions">
          <el-switch v-model="form.popupEnabled" active-text="进入首页弹窗" inactive-text="仅公告栏展示" />
          <el-button type="primary" :loading="saving" @click="save">{{ submitLabel }}</el-button>
        </div>
      </el-form>
    </div>

    <div class="panel notice-list">
      <div class="list-head">
        <div>
          <h2>公告列表</h2>
          <p>删除为逻辑删除，已过期或暂停公告不会出现在前台公告栏。</p>
        </div>
      </div>
      <el-table v-loading="loading" :data="notices" row-key="id">
        <el-table-column prop="title" label="标题" min-width="170" />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="展示状态" width="105">
          <template #default="{ row }">
            <el-tag :type="activeStatus(row).type" effect="plain">{{ activeStatus(row).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首页弹窗" width="100">
          <template #default="{ row }">
            <el-tag :type="row.popupEnabled === 1 ? 'warning' : 'info'" effect="plain">{{ row.popupEnabled === 1 ? '开启' : '关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="160" />
        <el-table-column prop="endTime" label="结束时间" min-width="160">
          <template #default="{ row }">{{ row.endTime || '长期有效' }}</template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" min-width="160" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editNotice(row)">编辑</el-button>
            <el-button link type="primary" @click="toggleStatus(row)">{{ row.publishStatus === 'PUBLISHED' ? '暂停' : '启用' }}</el-button>
            <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.notice-form,
.notice-list {
  margin-top: 16px;
}

.notice-form {
  margin-top: 0;
}

.form-head,
.list-head,
.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.form-head,
.list-head {
  margin-bottom: 14px;
}

.form-head h2,
.list-head h2 {
  margin: 0;
  font-size: 18px;
}

.form-head p,
.list-head p {
  margin: 4px 0 0;
  color: var(--muted);
}

.form-actions {
  flex-wrap: wrap;
}
</style>
