import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('../views/public/HomeView.vue') },
        { path: 'items', name: 'items', component: () => import('../views/public/ItemListView.vue') },
        { path: 'items/:id', name: 'item-detail', component: () => import('../views/public/ItemDetailView.vue') },
        { path: 'login', name: 'login', component: () => import('../views/auth/LoginView.vue') },
        { path: 'register', name: 'register', component: () => import('../views/auth/RegisterView.vue') }
      ]
    },
    {
      path: '/user',
      component: () => import('../layouts/UserLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'user-profile', component: () => import('../views/user/UserProfileView.vue') },
        { path: 'publish', name: 'user-publish', component: () => import('../views/user/PublishItemView.vue') },
        { path: 'items', name: 'user-items', component: () => import('../views/user/MyItemsView.vue') },
        { path: 'claims', name: 'user-claims', component: () => import('../views/user/MyClaimsView.vue') },
        { path: 'clues', name: 'user-clues', component: () => import('../views/user/MyCluesView.vue') },
        { path: 'notices', name: 'user-notices', component: () => import('../views/user/MyNoticesView.vue') }
      ]
    },
    {
      path: '/admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN', 'STAFF'] },
      children: [
        { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboardView.vue'), meta: { roles: ['ADMIN', 'STAFF'] } },
        { path: 'item-review', name: 'admin-item-review', component: () => import('../views/admin/AdminItemReviewView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'items', name: 'admin-items', component: () => import('../views/admin/AdminItemManageView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'claims', name: 'admin-claims', component: () => import('../views/admin/AdminClaimsView.vue'), meta: { roles: ['ADMIN', 'STAFF'] } },
        { path: 'clues', name: 'admin-clues', component: () => import('../views/admin/AdminCluesView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'handover', name: 'staff-handover', component: () => import('../views/admin/HandoverView.vue'), meta: { roles: ['ADMIN', 'STAFF'] } },
        { path: 'users', name: 'admin-users', component: () => import('../views/admin/UserManageView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'taxonomy', name: 'admin-taxonomy', component: () => import('../views/admin/TaxonomyManageView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'notices', name: 'admin-notices', component: () => import('../views/admin/NoticeManageView.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'logs', name: 'admin-logs', component: () => import('../views/admin/OperationLogView.vue'), meta: { roles: ['ADMIN'] } }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.isAuthed && !auth.user) {
    await auth.fetchMe().catch(() => auth.logout())
  }
  if (to.meta.requiresAuth && !auth.isAuthed) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles && !auth.hasAnyRole(roles)) {
    return { name: 'home' }
  }
  return true
})

export default router
