<script setup>
import { computed, shallowRef } from 'vue'
import { NButton, NEmpty, NIcon, NResult, NSpin } from 'naive-ui'
import {
  ArrowForwardOutline,
  CalendarOutline,
  ConstructOutline,
  DocumentTextOutline
} from '@vicons/ionicons5'

const props = defineProps({
  categories: { type: Array, default: () => [] },
  items: { type: Array, default: () => [] },
  searchText: { type: String, default: '' },
  loading: Boolean,
  errorMessage: { type: String, default: '' }
})

const emit = defineEmits(['apply', 'retry'])
const selectedCategory = shallowRef('all')

const visibleItems = computed(() => {
  const query = props.searchText.trim().toLocaleLowerCase('zh-CN')
  return props.items.filter((item) => {
    const matchesCategory = selectedCategory.value === 'all' || item.categoryId === selectedCategory.value
    const content = `${item.name || ''} ${item.description || ''} ${item.requiredMaterials || ''}`.toLocaleLowerCase('zh-CN')
    return matchesCategory && (!query || content.includes(query))
  })
})

const categoryNameMap = computed(() => Object.fromEntries(props.categories.map((category) => [category.id, category.name])))

function serviceMeta(type) {
  if (type === 'REPAIR') return { icon: ConstructOutline, tone: 'repair' }
  if (type === 'CERTIFICATE') return { icon: DocumentTextOutline, tone: 'certificate' }
  return { icon: CalendarOutline, tone: 'venue' }
}
</script>

<template>
  <section id="service-directory" class="service-band" aria-labelledby="service-directory-title">
    <div class="service-inner">
      <header class="section-heading">
        <h2 id="service-directory-title">办事大厅</h2>
        <p>按事项准备信息，提交后由对应部门在线办理。</p>
      </header>

      <div class="service-tabs" role="tablist" aria-label="服务分类">
        <button
          type="button"
          role="tab"
          :aria-selected="selectedCategory === 'all'"
          :class="{ active: selectedCategory === 'all' }"
          @click="selectedCategory = 'all'"
        >
          全部事项
        </button>
        <button
          v-for="category in categories"
          :key="category.id"
          type="button"
          role="tab"
          :aria-selected="selectedCategory === category.id"
          :class="{ active: selectedCategory === category.id }"
          @click="selectedCategory = category.id"
        >
          {{ category.name }}
        </button>
      </div>

      <div v-if="loading" class="service-state"><n-spin size="large" /></div>
      <n-result v-else-if="errorMessage" status="error" title="服务目录加载失败" :description="errorMessage">
        <template #footer><n-button @click="emit('retry')">重新加载</n-button></template>
      </n-result>
      <n-empty v-else-if="!visibleItems.length" :description="searchText ? '没有找到匹配事项' : '当前分类暂无可办理事项'" />

      <div v-else class="service-ledger">
        <article v-for="(item, index) in visibleItems" :key="item.id" class="service-row">
          <div class="service-number">{{ String(index + 1).padStart(2, '0') }}</div>
          <div :class="['service-icon', `service-icon-${serviceMeta(item.type).tone}`]">
            <n-icon :component="serviceMeta(item.type).icon" />
          </div>
          <div class="service-copy">
            <div class="service-title-line">
              <h3>{{ item.name }}</h3>
              <span>{{ categoryNameMap[item.categoryId] || '学生事务' }}</span>
            </div>
            <p>{{ item.description }}</p>
            <small>所需信息：{{ item.requiredMaterials || '按办理页面填写即可' }}</small>
          </div>
          <button class="service-action" type="button" @click="emit('apply', item.id)">
            在线办理
            <n-icon :component="ArrowForwardOutline" />
          </button>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.service-band {
  background: #fff;
}

.service-inner {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 64px 0 68px;
}

.section-heading h2 {
  margin: 0;
  font-family: "Songti SC", STSong, "Microsoft YaHei", serif;
  font-size: 32px;
  color: #173f37;
}

.section-heading p {
  margin: 10px 0 0;
  color: #697671;
  line-height: 1.7;
}

.service-tabs {
  display: flex;
  gap: 28px;
  margin: 30px 0 12px;
  overflow-x: auto;
  overflow-y: hidden;
  border-bottom: 1px solid #dfe5e2;
}

.service-tabs button {
  position: relative;
  flex: 0 0 auto;
  padding: 0 0 13px;
  border: 0;
  color: #6a7773;
  background: transparent;
  cursor: pointer;
}

.service-tabs button::after {
  content: "";
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 3px;
  background: transparent;
}

.service-tabs button.active {
  color: #173f37;
  font-weight: 700;
}

.service-tabs button.active::after {
  background: #a5413b;
}

.service-state {
  min-height: 260px;
  display: grid;
  place-items: center;
}

.service-ledger {
  border-bottom: 1px solid #dfe5e2;
}

.service-row {
  min-height: 148px;
  display: grid;
  grid-template-columns: 48px 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  padding: 24px 12px;
  border-top: 1px solid #dfe5e2;
  transition: background-color 160ms ease;
}

.service-row:hover {
  background: #f6f8f7;
}

.service-number {
  align-self: start;
  padding-top: 5px;
  color: #9aa5a1;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.service-icon {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  font-size: 28px;
}

.service-icon-repair {
  color: #91433d;
  background: #f5e9e7;
}

.service-icon-certificate {
  color: #246151;
  background: #e6f0ed;
}

.service-icon-venue {
  color: #2c5d7b;
  background: #e8f0f5;
}

.service-copy {
  min-width: 0;
}

.service-title-line {
  display: flex;
  align-items: baseline;
  gap: 14px;
  flex-wrap: wrap;
}

.service-title-line h3 {
  margin: 0;
  font-size: 20px;
  color: #23312e;
}

.service-title-line span {
  color: #7b8783;
  font-size: 13px;
}

.service-copy p {
  max-width: 720px;
  margin: 9px 0 7px;
  color: #5e6b67;
  line-height: 1.65;
}

.service-copy small {
  color: #818c88;
}

.service-action {
  min-width: 110px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 16px;
  border: 1px solid #205c50;
  border-radius: 4px;
  color: #205c50;
  background: #fff;
  font-weight: 700;
  cursor: pointer;
}

.service-action:hover,
.service-action:focus-visible {
  color: #fff;
  background: #205c50;
}

@media (max-width: 720px) {
  .service-inner {
    width: calc(100% - 28px);
    padding: 48px 0 52px;
  }

  .section-heading h2 {
    font-size: 28px;
  }

  .service-tabs {
    gap: 22px;
    margin-top: 24px;
  }

  .service-row {
    grid-template-columns: 42px minmax(0, 1fr);
    gap: 14px 12px;
    padding: 22px 0;
  }

  .service-number {
    display: none;
  }

  .service-icon {
    width: 42px;
    height: 42px;
    font-size: 23px;
  }

  .service-action {
    grid-column: 2;
    justify-self: start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .service-row {
    transition: none;
  }
}
</style>
