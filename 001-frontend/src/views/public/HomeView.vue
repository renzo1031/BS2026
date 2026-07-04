<script setup lang="ts">
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, EditPen, Location, Search, Tickets } from '@element-plus/icons-vue'
import { catalogApi, itemApi } from '../../api/modules'
import type { Category, Item, Location as CampusLocation, Notice } from '../../types'

const router = useRouter()
const loading = shallowRef(false)
const popupVisible = shallowRef(false)
const items = ref<Item[]>([])
const notices = ref<Notice[]>([])
const categories = ref<Category[]>([])
const locations = ref<CampusLocation[]>([])
const popupNotice = ref<Notice | null>(null)
const totalItems = shallowRef(0)
const searchForm = reactive({ keyword: '', type: '' })

const categoryMap = computed(() => Object.fromEntries(categories.value.map((item) => [item.id, item.categoryName])))
const locationMap = computed(() => Object.fromEntries(locations.value.map((item) => [item.id, item.locationName])))
const foundCount = computed(() => items.value.filter((item) => item.type === 'FOUND').length)
const lostCount = computed(() => items.value.filter((item) => item.type === 'LOST').length)
const firstNotice = computed(() => notices.value[0])
const heroStats = computed(() => [
  { label: '公开物品', value: totalItems.value },
  { label: '招领线索', value: foundCount.value },
  { label: '寻物登记', value: lostCount.value },
  { label: '服务地点', value: locations.value.length }
])

const quickActions = [
  { title: '发布登记', desc: '丢了或捡到物品，都先进入登记审核流程。', icon: EditPen, path: '/user/publish', primary: true },
  { title: '查找物品', desc: '按关键词、分类和地点筛选公开记录。', icon: Search, path: '/items' },
  { title: '我的进度', desc: '查看发布、认领、线索和系统通知。', icon: Tickets, path: '/user' },
  { title: '公告通知', desc: '查看校园失物招领规则和系统提醒。', icon: Bell, path: '/user/notices' }
]

const processSteps = [
  { title: '登记信息', desc: '填写类型、地点、时间、联系方式和物品特征。' },
  { title: '后台审核', desc: '管理员检查重复、虚假或信息不完整记录。' },
  { title: '公开查找', desc: '审核通过后进入前台列表，支持同学搜索。' },
  { title: '认领核验', desc: '申请人提交能证明归属的材料，后台核验。' },
  { title: '线下交接', desc: '保管员记录领取人、位置和经办结果后闭环。' }
]

const claimTips = [
  '认领时准备学号、联系方式和能证明归属的描述或凭证。',
  '贵重物品建议到校门口服务中心或指定保管点线下核验。',
  '不要在描述里公开完整身份证、银行卡、校园卡敏感号码。',
  '发现疑似本人遗失物，可先提交线索或直接发起认领申请。'
]

async function load() {
  loading.value = true
  try {
    const [itemPage, noticeList, categoryList, locationList] = await Promise.all([
      itemApi.page({ pageNum: 1, pageSize: 8 }),
      catalogApi.notices(),
      catalogApi.categories(),
      catalogApi.locations()
    ])
    items.value = itemPage.records
    totalItems.value = itemPage.total
    notices.value = noticeList
    categories.value = categoryList
    locations.value = locationList
    openPopupNotice(noticeList)
  } finally {
    loading.value = false
  }
}

function openPopupNotice(noticeList: Notice[]) {
  const notice = noticeList.find((item) => item.popupEnabled === 1)
  if (!notice) return
  const storageKey = `home-popup-notice-${notice.id}`
  if (sessionStorage.getItem(storageKey)) return
  popupNotice.value = notice
  popupVisible.value = true
  sessionStorage.setItem(storageKey, '1')
}

function typeLabel(type?: string) {
  return type === 'LOST' ? '寻物' : '招领'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function itemMeta(item: Item) {
  const category = categoryMap.value[item.categoryId] || '未分类'
  const locationName = locationMap.value[item.locationId] || '未标注地点'
  return `${category} · ${locationName} · ${formatDateTime(item.eventTime)}`
}

function goItems(extra: Record<string, string> = {}) {
  router.push({
    path: '/items',
    query: {
      keyword: searchForm.keyword || undefined,
      type: searchForm.type || undefined,
      ...extra
    }
  })
}

function goPath(path: string) {
  router.push(path)
}

onMounted(load)
</script>

<template>
  <section class="home page-shell">
    <div class="hero">
      <div class="hero-main">
        <p class="eyebrow">阳光校园 · 真实可追踪</p>
        <h1>让每一件遗失和拾到的物品都有去向</h1>
        <p class="hero-copy">
          从登记、审核、公开查找，到认领核验、线下交接和归档，系统把每一步都留痕，方便同学查找，也方便后台追责。
        </p>
        <div class="search-strip">
          <el-input v-model="searchForm.keyword" size="large" placeholder="搜索物品名称、编号或描述" clearable @keyup.enter="goItems()" />
          <el-select v-model="searchForm.type" size="large" placeholder="类型" clearable>
            <el-option label="招领" value="FOUND" />
            <el-option label="寻物" value="LOST" />
          </el-select>
          <el-button size="large" type="primary" :icon="Search" @click="goItems()">搜索</el-button>
        </div>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="goPath('/user/publish')">发布登记</el-button>
          <el-button size="large" @click="goPath('/items')">查看全部</el-button>
        </div>
      </div>
      <div class="hero-side">
        <div v-for="stat in heroStats" :key="stat.label" class="hero-stat">
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <div class="quick-grid">
      <button v-for="action in quickActions" :key="action.title" class="quick-card" type="button" @click="goPath(action.path)">
        <el-icon><component :is="action.icon" /></el-icon>
        <strong>{{ action.title }}</strong>
        <span>{{ action.desc }}</span>
      </button>
    </div>

    <div class="home-grid">
      <section class="panel latest-panel">
        <div class="section-head">
          <div>
            <h2 class="section-title">最新公开物品</h2>
            <p>优先展示最近通过审核并上架的记录。</p>
          </div>
          <el-button @click="goPath('/items')">全部物品</el-button>
        </div>
        <el-skeleton v-if="loading" :rows="5" animated />
        <div v-else class="item-list">
          <RouterLink v-for="item in items" :key="item.id" :to="`/items/${item.id}`" class="item-row">
            <span class="tag" :class="item.type === 'LOST' ? 'tag-lost' : 'tag-found'">{{ typeLabel(item.type) }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ itemMeta(item) }}</p>
            </div>
            <span class="item-status">{{ item.lastOperationSummary || '已上架' }}</span>
          </RouterLink>
          <el-empty v-if="!items.length" description="暂无公开物品" />
        </div>
      </section>

      <aside class="side-stack">
        <section class="panel notice-panel">
          <div class="section-head compact">
            <h2 class="section-title">公告</h2>
            <el-button text type="primary" @click="goPath('/user/notices')">查看</el-button>
          </div>
          <div v-if="firstNotice" class="notice-feature">
            <strong>{{ firstNotice.title }}</strong>
            <p>{{ firstNotice.content }}</p>
          </div>
          <div v-for="notice in notices.slice(1, 3)" :key="notice.id" class="notice-row">
            <strong>{{ notice.title }}</strong>
            <span>{{ formatDateTime(notice.publishedAt) }}</span>
          </div>
          <el-empty v-if="!notices.length" description="暂无公告" />
        </section>

        <section class="panel help-panel">
          <h2 class="section-title">认领须知</h2>
          <ul>
            <li v-for="tip in claimTips" :key="tip">{{ tip }}</li>
          </ul>
        </section>
      </aside>
    </div>

    <section class="panel process-panel">
      <div class="section-head">
        <div>
          <h2 class="section-title">完整业务流程</h2>
          <p>首页给同学看入口，后台负责审核、核验、交接和审计。</p>
        </div>
      </div>
      <div class="process-list">
        <div v-for="(step, index) in processSteps" :key="step.title" class="process-step">
          <span>{{ index + 1 }}</span>
          <strong>{{ step.title }}</strong>
          <p>{{ step.desc }}</p>
        </div>
      </div>
    </section>

    <div class="catalog-grid">
      <section class="panel">
        <div class="section-head">
          <div>
            <h2 class="section-title">常见分类</h2>
            <p>按物品类型快速缩小范围。</p>
          </div>
        </div>
        <div class="chip-list">
          <button v-for="category in categories" :key="category.id" type="button" @click="goItems({ categoryId: String(category.id) })">
            {{ category.categoryName }}
          </button>
        </div>
      </section>

      <section class="panel">
        <div class="section-head">
          <div>
            <h2 class="section-title">校园地点</h2>
            <p>高频遗失地点可以直接进入筛选。</p>
          </div>
        </div>
        <div class="location-list">
          <button v-for="place in locations.slice(0, 8)" :key="place.id" type="button" @click="goItems({ locationId: String(place.id) })">
            <el-icon><Location /></el-icon>
            <span>{{ place.locationName }}</span>
            <small>{{ place.areaName || '校园区域' }}</small>
          </button>
        </div>
      </section>
    </div>

    <el-dialog v-model="popupVisible" width="520px" class="notice-dialog" align-center>
      <template #header>
        <div class="notice-dialog-title">
          <span>校园公告</span>
          <strong>{{ popupNotice?.title }}</strong>
        </div>
      </template>
      <p class="notice-dialog-content">{{ popupNotice?.content }}</p>
      <template #footer>
        <el-button type="primary" @click="popupVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  min-height: 360px;
  border-radius: 8px;
  padding: 42px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 30px;
  align-items: center;
  background:
    linear-gradient(135deg, rgba(228, 243, 255, 0.94) 0%, rgba(255, 247, 223, 0.9) 56%, rgba(233, 248, 238, 0.94) 100%),
    repeating-linear-gradient(90deg, rgba(27, 117, 187, 0.06) 0, rgba(27, 117, 187, 0.06) 1px, transparent 1px, transparent 26px);
  border: 1px solid var(--line);
}

.hero-main {
  min-width: 0;
}

.eyebrow {
  color: var(--brand);
  font-weight: 700;
  margin: 0 0 8px;
}

.hero h1 {
  max-width: 760px;
  margin: 0 0 12px;
  font-size: 42px;
  line-height: 1.18;
  letter-spacing: 0;
}

.hero-copy {
  max-width: 760px;
  margin: 0;
  color: #49657f;
  line-height: 1.8;
}

.search-strip {
  max-width: 820px;
  margin-top: 22px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px 100px;
  gap: 10px;
}

.hero-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-side {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.hero-stat {
  min-height: 118px;
  border-radius: 8px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.86);
}

.hero-stat strong {
  display: block;
  font-size: 40px;
  color: var(--brand);
  line-height: 1;
}

.hero-stat span {
  margin-top: 10px;
  color: #49657f;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-card {
  min-height: 136px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 18px;
  text-align: left;
  background: #fff;
  color: #1f2a37;
  cursor: pointer;
}

.quick-card .el-icon {
  width: 34px;
  height: 34px;
  margin-bottom: 14px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--brand);
}

.quick-card strong,
.quick-card span {
  display: block;
}

.quick-card span {
  margin-top: 8px;
  color: var(--muted);
  line-height: 1.6;
}

.home-grid,
.catalog-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.75fr);
  gap: 18px;
}

.catalog-grid {
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head.compact {
  align-items: center;
}

.section-head p {
  margin: 4px 0 0;
  color: var(--muted);
}

.side-stack {
  display: grid;
  gap: 18px;
}

.item-list {
  display: grid;
  gap: 0;
}

.item-row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) minmax(160px, 220px);
  gap: 12px;
  align-items: center;
  padding: 13px 0;
  border-bottom: 1px solid var(--line);
}

.item-row strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-row p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.tag {
  color: #fff;
  background: var(--brand);
  border-radius: 6px;
  padding: 5px 8px;
  text-align: center;
  font-weight: 700;
}

.tag-lost {
  background: #d92d20;
}

.tag-found {
  background: var(--brand);
}

.item-status {
  color: var(--muted);
  font-size: 13px;
  text-align: right;
}

.notice-feature {
  padding: 14px;
  border-radius: 8px;
  background: #f4f9ff;
  border: 1px solid #dce9f5;
}

.notice-feature strong,
.notice-row strong {
  display: block;
}

.notice-feature p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.notice-row {
  padding: 12px 0;
  border-bottom: 1px solid var(--line);
}

.notice-row span {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.help-panel ul {
  margin: 0;
  padding-left: 18px;
  color: #344054;
  line-height: 1.8;
}

.process-list {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.process-step {
  padding: 16px;
  border-radius: 8px;
  background: #f8fbff;
  border: 1px solid #e6edf5;
}

.process-step span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  color: #fff;
  background: var(--brand);
  font-weight: 700;
}

.process-step strong {
  display: block;
  margin-top: 12px;
}

.process-step p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip-list button,
.location-list button {
  border: 1px solid #dce9f5;
  border-radius: 8px;
  background: #fff;
  color: #1f2a37;
  cursor: pointer;
}

.chip-list button {
  padding: 9px 12px;
}

.location-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.location-list button {
  min-width: 0;
  padding: 12px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 2px 8px;
  align-items: center;
  text-align: left;
}

.location-list .el-icon {
  grid-row: 1 / span 2;
  color: var(--brand);
}

.location-list span,
.location-list small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-list small {
  color: var(--muted);
}

.notice-dialog-title {
  display: grid;
  gap: 6px;
}

.notice-dialog-title span {
  color: var(--brand);
  font-weight: 700;
  font-size: 13px;
}

.notice-dialog-title strong {
  font-size: 20px;
}

.notice-dialog-content {
  margin: 0;
  color: #344054;
  line-height: 1.8;
}

@media (max-width: 1020px) {
  .hero,
  .home-grid,
  .catalog-grid {
    grid-template-columns: 1fr;
  }

  .quick-grid,
  .process-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero {
    padding: 24px;
  }

  .hero h1 {
    font-size: 32px;
  }

  .search-strip,
  .quick-grid,
  .process-list,
  .location-list,
  .item-row {
    grid-template-columns: 1fr;
  }

  .item-status {
    text-align: left;
  }
}
</style>
