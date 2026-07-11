<script setup lang="ts">
import { ArrowRightOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import heroImage from '@/assets/community-care-hero.webp'
</script>

<template>
  <section class="home-hero" :style="{ backgroundImage: `url(${heroImage})` }">
    <div class="hero-shade" aria-hidden="true"></div>
    <div class="container hero-content">
      <p class="hero-eyebrow"><SafetyCertificateOutlined /> 儿童关爱协作平台</p>
      <h1>童伴关爱</h1>
      <p class="hero-lead">让每一次需要被看见，让每一份帮助有回应、有记录、有结果。</p>
      <div class="hero-actions">
        <a-button type="primary" size="large" href="/aid-hall">
          查看帮扶需求
          <template #icon><ArrowRightOutlined /></template>
        </a-button>
        <a-button class="hero-secondary" size="large" href="/register">成为志愿者</a-button>
      </div>
      <p class="hero-note">平台公开内容均已脱敏，儿童个人资料仅对授权工作人员和已分配志愿者开放。</p>
    </div>
  </section>
</template>

<style scoped>
.home-hero {
  position: relative;
  min-height: 540px;
  height: clamp(540px, calc(100vh - 110px), 720px);
  display: flex;
  align-items: center;
  overflow: hidden;
  background-position: center;
  background-size: cover;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: rgba(15, 48, 39, 0.56);
}

.hero-content {
  position: relative;
  z-index: 1;
  color: #ffffff;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
  font-size: 16px;
  font-weight: 600;
}

.hero-content h1 {
  margin: 0;
  font-size: 56px;
  line-height: 1.12;
  letter-spacing: 0;
}

.hero-lead {
  max-width: 640px;
  margin: 20px 0 0;
  font-size: 24px;
  line-height: 1.6;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
}

.hero-actions :deep(.ant-btn) {
  min-width: 150px;
}

.hero-secondary {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.8);
}

.hero-secondary:hover {
  color: #173d33 !important;
  background: #ffffff !important;
  border-color: #ffffff !important;
}

.hero-note {
  max-width: 680px;
  margin: 26px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

@media (max-width: 760px) {
  .home-hero {
    min-height: 560px;
    height: calc(100vh - 96px);
    max-height: 680px;
    background-position: 45% center;
  }

  .hero-content h1 {
    font-size: 42px;
  }

  .hero-lead {
    font-size: 20px;
  }

  .hero-actions {
    align-items: stretch;
    flex-direction: column;
    max-width: 260px;
  }
}
</style>
