<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, CheckCircleOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { api } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { PublicAidView } from '@/types/models'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const notFound = shallowRef(false)
const applyOpen = shallowRef(false)
const submitting = shallowRef(false)
const detail = shallowRef<PublicAidView | null>(null)
const form = reactive({ message: '' })
const inWorkspace = computed(() => route.path.startsWith('/app'))
const backPath = computed(() => inWorkspace.value ? '/app/aid-hall' : '/aid-hall')
const isVolunteer = computed(() => auth.user?.roleCode === 'VOLUNTEER')

const categoryLabels: Record<string, string> = {
  EDUCATION: '学习支持', COMPANIONSHIP: '成长陪伴', LIFE_CARE: '生活关怀',
  SAFETY: '安全关爱', PSYCHOLOGICAL: '心理支持', OTHER: '其他支持',
}

async function load() {
  loading.value = true
  try {
    detail.value = await api.get<PublicAidView>(`/public/aid-requests/${route.params.id}`)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function apply() {
  if (!detail.value) return
  submitting.value = true
  try {
    await api.post<void>(`/aid-requests/${detail.value.id}/applications`, form)
    message.success('申请已提交')
    applyOpen.value = false
    await router.push('/app/applications')
  } finally {
    submitting.value = false
  }
}

function openApply() {
  if (!auth.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!isVolunteer.value) {
    message.warning('该入口仅面向志愿者账号')
    return
  }
  applyOpen.value = true
}

onMounted(load)
</script>

<template>
  <section :class="['aid-detail-page', { workspace: inWorkspace }]">
    <div :class="{ container: !inWorkspace }">
      <a-button type="text" @click="router.push(backPath)"><ArrowLeftOutlined /> 返回需求大厅</a-button>
      <a-spin :spinning="loading">
        <a-result v-if="notFound" status="404" title="需求不存在或已完成匹配" />
        <article v-else-if="detail" class="detail-surface">
          <header class="detail-header">
            <div>
              <a-tag :color="detail.priority === 'URGENT' ? 'red' : 'green'">
                {{ detail.priority === 'URGENT' ? '紧急需求' : categoryLabels[detail.category] }}
              </a-tag>
              <h1>{{ detail.title }}</h1>
              <p>{{ detail.requestNo }} · {{ detail.region }} · {{ detail.ageGroup }}</p>
            </div>
            <a-button type="primary" size="large" @click="openApply">申请参与</a-button>
          </header>
          <div class="detail-body">
            <section>
              <h2>需求摘要</h2>
              <p>{{ detail.summary }}</p>
            </section>
            <aside>
              <h2><SafetyCertificateOutlined /> 信息保护</h2>
              <ul>
                <li><CheckCircleOutlined /> 当前页面不展示儿童姓名和联系方式</li>
                <li><CheckCircleOutlined /> 申请由负责机构统一审核匹配</li>
                <li><CheckCircleOutlined /> 匹配成功后按最小必要范围提供联系信息</li>
              </ul>
            </aside>
          </div>
        </article>
      </a-spin>
    </div>
    <a-modal v-model:open="applyOpen" title="提交志愿申请" :confirm-loading="submitting" @ok="apply">
      <a-form layout="vertical">
        <a-form-item label="申请说明" required>
          <a-textarea v-model:value="form.message" :rows="5" :maxlength="500" show-count placeholder="请简要说明可提供的帮助和可服务时间" />
        </a-form-item>
      </a-form>
    </a-modal>
  </section>
</template>

<style scoped>
.aid-detail-page {
  min-height: 620px;
  padding: 44px 0 80px;
  background: #f4f7f6;
}

.aid-detail-page.workspace {
  min-height: 0;
  padding: 0;
}

.detail-surface {
  margin-top: 16px;
  background: #ffffff;
  border: 1px solid #d9e0e4;
  border-radius: 6px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 30px;
  border-bottom: 1px solid #e1e7e4;
}

.detail-header h1 {
  margin: 16px 0 8px;
  font-size: 28px;
}

.detail-header p {
  margin: 0;
  color: #65717d;
}

.detail-body {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 36px;
  padding: 34px 30px 44px;
}

.detail-body h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.detail-body p {
  margin: 0;
  color: #4e5c57;
  line-height: 1.9;
  white-space: pre-wrap;
}

.detail-body aside {
  padding-left: 30px;
  border-left: 1px solid #e1e7e4;
}

.detail-body ul {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  color: #52615b;
  line-height: 1.7;
  list-style: none;
}

.detail-body li :deep(.anticon) {
  margin-right: 6px;
  color: #087f5b;
}

@media (max-width: 720px) {
  .detail-header,
  .detail-body {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .detail-header :deep(.ant-btn) {
    width: 100%;
  }

  .detail-body aside {
    padding: 24px 0 0;
    border-top: 1px solid #e1e7e4;
    border-left: 0;
  }
}
</style>
