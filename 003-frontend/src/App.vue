<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: '#087f5b',
    colorInfo: '#2563eb',
    colorSuccess: '#2f9e44',
    colorWarning: '#e67700',
    colorError: '#d9485f',
    colorText: '#17212b',
    colorTextSecondary: '#56616d',
    colorBorder: '#d9e0e4',
    colorBgLayout: '#f4f7f6',
    borderRadius: 6,
    controlHeight: 38,
    fontSize: 14,
    wireframe: false,
  },
  components: {
    Layout: {
      colorBgHeader: '#ffffff',
      colorBgBody: '#f4f7f6',
      colorBgTrigger: '#18302b',
    },
    Menu: {
      colorItemBg: '#18302b',
      colorItemBgSelected: '#087f5b',
      colorSubItemBg: '#132722',
      colorItemText: '#dce8e4',
      colorItemTextSelected: '#ffffff',
      radiusItem: 4,
      radiusSubMenuItem: 4,
    },
  },
}
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <RouterView />
  </a-config-provider>
</template>
