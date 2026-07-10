<script setup>
import { useAuthStore } from '../store/auth'
import MainLayout from './MainLayout.vue'
import StudentPortalLayout from './StudentPortalLayout.vue'

const auth = useAuthStore()
</script>

<template>
  <StudentPortalLayout v-if="auth.isStudent" />
  <MainLayout v-else />
</template>
