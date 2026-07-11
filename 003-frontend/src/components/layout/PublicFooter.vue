<script setup lang="ts">
import { SafetyCertificateOutlined } from '@ant-design/icons-vue'
</script>

<template>
  <footer class="public-footer">
    <div class="container footer-grid">
      <div>
        <h2 class="footer-brand">童伴关爱</h2>
        <p>面向学校、社区与社会工作机构的儿童关爱协作平台。</p>
      </div>
      <div>
        <h3>平台服务</h3>
        <RouterLink to="/#services">服务内容</RouterLink>
        <RouterLink to="/aid-hall">帮扶需求</RouterLink>
      </div>
      <div>
        <h3>参与平台</h3>
        <RouterLink to="/register">志愿者注册</RouterLink>
        <RouterLink to="/login">工作人员登录</RouterLink>
      </div>
      <div class="privacy-note">
        <SafetyCertificateOutlined />
        <span>公开内容均经过脱敏处理，不展示儿童个人身份信息。</span>
      </div>
    </div>
    <div class="footer-bottom">© 2026 童伴关爱</div>
  </footer>
</template>

<style scoped>
.public-footer {
  color: #dce8e4;
  background: #142c27;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.5fr;
  gap: 36px;
  padding: 50px 0 36px;
}

.footer-brand,
.footer-grid h3 {
  margin: 0 0 14px;
  color: #ffffff;
}

.footer-brand {
  font-size: 22px;
}

.footer-grid p {
  max-width: 420px;
  margin: 0;
  color: #a9beb7;
  line-height: 1.8;
}

.footer-grid a {
  display: block;
  margin: 10px 0;
  color: #bdcec8;
  text-decoration: none;
}

.footer-grid a:hover {
  color: #ffffff;
}

.privacy-note {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  line-height: 1.7;
  color: #b9cbc5;
}

.privacy-note :deep(.anticon) {
  margin-top: 4px;
  color: #68c69d;
  font-size: 20px;
}

.footer-bottom {
  padding: 18px 20px;
  text-align: center;
  color: #8ea59d;
  border-top: 1px solid #29443d;
}

@media (max-width: 820px) {
  .footer-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 520px) {
  .footer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
