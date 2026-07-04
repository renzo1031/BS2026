<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { catalogApi, itemApi } from '../../api/modules'
import type { Category, Location } from '../../types'

const router = useRouter()
const loading = shallowRef(false)
const categories = ref<Category[]>([])
const locations = ref<Location[]>([])
const form = reactive({
  type: 'FOUND',
  title: '',
  categoryId: '',
  locationId: '',
  eventTime: '',
  description: '',
  contactName: '',
  contactPhone: ''
})

async function submit(asReview: boolean) {
  loading.value = true
  try {
    const item = await itemApi.create(form)
    if (asReview) {
      await itemApi.submit(item.id)
    }
    ElMessage.success(asReview ? '已提交审核' : '草稿已保存')
    router.push('/user/items')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const [categoryList, locationList] = await Promise.all([catalogApi.categories(), catalogApi.locations()])
  categories.value = categoryList
  locations.value = locationList
})
</script>

<template>
  <section>
    <h1 class="section-title">发布登记</h1>
    <el-form label-position="top" class="panel" @submit.prevent>
      <div class="form-grid">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button label="FOUND">我拾到了</el-radio-button>
            <el-radio-button label="LOST">我丢失了</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width: 100%">
            <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点">
          <el-select v-model="form.locationId" style="width: 100%">
            <el-option v-for="item in locations" :key="item.id" :label="item.locationName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间"><el-date-picker v-model="form.eventTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
      </div>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="5" />
      </el-form-item>
      <el-button :loading="loading" @click="submit(false)">保存草稿</el-button>
      <el-button type="primary" :loading="loading" @click="submit(true)">提交审核</el-button>
    </el-form>
  </section>
</template>
