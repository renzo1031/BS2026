<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import { CalendarOutlined, EnvironmentOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { api } from '@/api/http'
import type { PageResult, PublicAidView } from '@/types/models'

const route = useRoute()
const loading = shallowRef(false)
const data = shallowRef<PageResult<PublicAidView>>({ items: [], total: 0, page: 1, size: 9 })
const filters = reactive({ keyword: '', category: undefined as string | undefined })
const inWorkspace = computed(() => route.path.startsWith('/app'))

const categories: Record<string, string> = {
  EDUCATION: '学习支持',
  COMPANIONSHIP: '成长陪伴',
  LIFE_CARE: '生活关怀',
  SAFETY: '安全关爱',
  PSYCHOLOGICAL: '心理支持',
  OTHER: '其他支持',
}

async function load(page = 1) {
  loading.value = true
  try {
    data.value = await api.get<PageResult<PublicAidView>>('/public/aid-requests', {
      page,
      size: data.value.size,
      keyword: filters.keyword || undefined,
      category: filters.category,
    })
  } finally {
    loading.value = false
  }
}

function detailPath(id: number) {
  return inWorkspace.value ? `/app/aid-hall/${id}` : `/aid-hall/${id}`
}

onMounted(() => load())
</script>

<template>
  <section :class="['aid-hall', { workspace: inWorkspace }]">
    <div :class="{ container: !inWorkspace }">
      <header class="hall-header">
        <p>公开需求</p>
        <h1>帮扶需求大厅</h1>
        <span>仅展示审核通过的脱敏摘要，匹配完成后自动停止公开。</span>
      </header>
      <div class="hall-filters surface">
        <a-input v-model:value="filters.keyword" allow-clear placeholder="搜索需求标题或编号" @press-enter="load(1)">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-select v-model:value="filters.category" allow-clear placeholder="全部服务类型" :options="Object.entries(categories).map(([value, label]) => ({ value, label }))" />
        <a-button type="primary" @click="load(1)">查询</a-button>
      </div>
      <a-spin :spinning="loading">
        <div v-if="data.items.length" class="aid-grid">
          <article v-for="item in data.items" :key="item.id" class="aid-card">
            <div class="aid-card-top">
              <a-tag :color="item.priority === 'URGENT' ? 'red' : 'green'">
                {{ item.priority === 'URGENT' ? '紧急' : categories[item.category] }}
              </a-tag>
              <span>{{ item.requestNo }}</span>
            </div>
            <h2>{{ item.title }}</h2>
            <p class="aid-summary">{{ item.summary }}</p>
            <div class="aid-meta">
              <span><EnvironmentOutlined /> {{ item.region }}</span>
              <span><CalendarOutlined /> {{ item.ageGroup }}</span>
            </div>
            <RouterLink class="detail-link" :to="detailPath(item.id)">查看详情</RouterLink>
          </article>
        </div>
        <a-empty v-else description="暂无符合条件的公开需求" />
      </a-spin>
      <a-pagination
        v-if="data.total > data.size"
        class="hall-pagination"
        :current="data.page"
        :page-size="data.size"
        :total="data.total"
        :show-size-changer="false"
        @change="load"
      />
    </div>
  </section>
</template>

<style scoped>
.aid-hall {
  min-height: 620px;
  padding: 64px 0 80px;
  background: #f4f7f6;
}

.aid-hall.workspace {
  padding: 0;
  min-height: 0;
}

.hall-header {
  max-width: 680px;
  margin-bottom: 28px;
}

.hall-header p {
  margin: 0 0 8px;
  color: #087f5b;
  font-weight: 700;
}

.hall-header h1 {
  margin: 0;
  font-size: 34px;
}

.hall-header span {
  display: block;
  margin-top: 10px;
  color: #65717d;
}

.workspace .hall-header h1 {
  font-size: 24px;
}

.hall-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 220px auto;
  gap: 10px;
  padding: 16px;
  margin-bottom: 20px;
}

.aid-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.aid-card {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 276px;
  padding: 22px;
  background: #ffffff;
  border: 1px solid #d9e0e4;
  border-radius: 6px;
  transition: border-color 160ms ease, transform 160ms ease;
}

.aid-card:hover {
  border-color: #84b7a5;
  transform: translateY(-2px);
}

.aid-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  color: #7a8580;
  font-size: 12px;
}

.aid-card h2 {
  margin: 20px 0 10px;
  font-size: 19px;
}

.aid-summary {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: #65717d;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.aid-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: auto;
  padding-top: 20px;
  color: #52605b;
  font-size: 13px;
}

.detail-link {
  margin-top: 18px;
  color: #087f5b;
  font-weight: 600;
  text-decoration: none;
}

.hall-pagination {
  margin-top: 28px;
  text-align: center;
}

@media (max-width: 900px) {
  .aid-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 620px) {
  .aid-hall {
    padding: 42px 0 60px;
  }

  .hall-filters,
  .aid-grid {
    grid-template-columns: 1fr;
  }
}
</style>
