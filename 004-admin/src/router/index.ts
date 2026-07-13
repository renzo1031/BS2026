import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { AUTH_EXPIRED_EVENT } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import type { UserRole } from '@/types/api'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: AdminLayout,
    children: [
      { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'operations/activities', name: 'activity-management', component: () => import('@/views/ActivityManagementView.vue'), meta: { title: '活动列表' } },
      { path: 'reviews/activities', name: 'activity-reviews', component: () => import('@/views/ActivityReviewView.vue'), meta: { title: '活动审核' } },
      { path: 'reviews/identities', name: 'identity-reviews', component: () => import('@/views/IdentityReviewView.vue'), meta: { title: '身份审核' } },
      { path: 'governance/reports', name: 'report-reviews', component: () => import('@/views/ReportReviewView.vue'), meta: { title: '举报与申诉' } },
      { path: 'reviews/files', name: 'file-reviews', component: () => import('@/views/FileReviewView.vue'), meta: { title: '文件审核' } },
      {
        path: 'platform/users',
        name: 'platform-users',
        component: () => import('@/views/UserManagementView.vue'),
        meta: { title: '用户与审核员', roles: ['PLATFORM_ADMIN'] satisfies UserRole[] },
      },
      {
        path: 'platform/campuses',
        name: 'platform-campuses',
        component: () => import('@/views/CampusManagementView.vue'),
        meta: { title: '校园管理', roles: ['PLATFORM_ADMIN'] satisfies UserRole[] },
      },
      {
        path: 'platform/tags',
        name: 'platform-tags',
        component: () => import('@/views/TagManagementView.vue'),
        meta: { title: '推荐标签', roles: ['PLATFORM_ADMIN'] satisfies UserRole[] },
      },
      {
        path: 'platform/audits',
        name: 'audit-logs',
        component: () => import('@/views/AuditLogView.vue'),
        meta: { title: '审计日志', roles: ['PLATFORM_ADMIN'] satisfies UserRole[] },
      },
      { path: 'forbidden', name: 'forbidden', component: () => import('@/views/ForbiddenView.vue'), meta: { title: '无权限' } },
      { path: ':pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) return auth.authenticated ? { name: 'dashboard' } : true
  if (!auth.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  const roles = to.meta.roles as UserRole[] | undefined
  if (!auth.can(roles)) return { name: 'forbidden' }
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '管理台')} · 校园搭子`
})

window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  const current = router.currentRoute.value
  if (current.name === 'login') return
  void router.replace({ name: 'login', query: { redirect: current.fullPath } })
})

export default router
