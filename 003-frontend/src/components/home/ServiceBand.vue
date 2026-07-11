<script setup lang="ts">
import {
  BookOutlined,
  HeartOutlined,
  HomeOutlined,
  SafetyOutlined,
} from '@ant-design/icons-vue'

const services = [
  { title: '学习支持', description: '围绕课业辅导、阅读陪伴和兴趣拓展，建立持续服务记录。', icon: BookOutlined, color: '#2563eb' },
  { title: '成长陪伴', description: '通过稳定、规范的陪伴活动，支持儿童建立积极的社会连接。', icon: HeartOutlined, color: '#d9485f' },
  { title: '生活关怀', description: '由个案人员评估实际需要，匹配可执行、可验收的生活支持。', icon: HomeOutlined, color: '#e67700' },
  { title: '安全关爱', description: '围绕日常安全教育和风险发现，形成机构协作与跟进闭环。', icon: SafetyOutlined, color: '#087f5b' },
]
</script>

<template>
  <section id="services" class="service-band">
    <div class="container">
      <div class="section-heading">
        <p>服务内容</p>
        <h2>从真实需要出发，提供持续而有边界的支持</h2>
      </div>
      <div class="service-grid">
        <article v-for="service in services" :key="service.title" class="service-item">
          <span class="service-icon" :style="{ color: service.color, borderColor: service.color }">
            <component :is="service.icon" />
          </span>
          <h3>{{ service.title }}</h3>
          <p>{{ service.description }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.service-band {
  padding: 82px 0;
  background: #ffffff;
}

.section-heading {
  max-width: 720px;
  margin-bottom: 38px;
}

.section-heading > p {
  margin: 0 0 10px;
  color: #087f5b;
  font-weight: 700;
}

.section-heading h2 {
  margin: 0;
  color: #17212b;
  font-size: 34px;
  line-height: 1.4;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid #dce4e1;
  border-bottom: 1px solid #dce4e1;
}

.service-item {
  min-width: 0;
  padding: 34px 26px;
  border-right: 1px solid #dce4e1;
}

.service-item:last-child {
  border-right: 0;
}

.service-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  font-size: 23px;
  border: 1px solid;
  border-radius: 6px;
}

.service-item h3 {
  margin: 22px 0 10px;
  font-size: 19px;
}

.service-item p {
  margin: 0;
  color: #65717d;
  line-height: 1.8;
}

@media (max-width: 860px) {
  .service-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .service-item:nth-child(2) {
    border-right: 0;
  }

  .service-item:nth-child(-n + 2) {
    border-bottom: 1px solid #dce4e1;
  }
}

@media (max-width: 520px) {
  .service-band {
    padding: 62px 0;
  }

  .section-heading h2 {
    font-size: 28px;
  }

  .service-grid {
    grid-template-columns: 1fr;
  }

  .service-item,
  .service-item:nth-child(2) {
    border-right: 0;
    border-bottom: 1px solid #dce4e1;
  }

  .service-item:last-child {
    border-bottom: 0;
  }
}
</style>
