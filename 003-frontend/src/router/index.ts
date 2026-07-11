import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, top: 72, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue'), meta: { title: '童伴关爱' } },
        { path: 'aid-hall', name: 'public-aid-hall', component: () => import('@/views/public/AidHallView.vue'), meta: { title: '帮扶需求' } },
        { path: 'aid-hall/:id', name: 'public-aid-detail', component: () => import('@/views/public/AidDetailView.vue'), meta: { title: '需求详情' } },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        { path: 'login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guestOnly: true, title: '登录' } },
        { path: 'register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { guestOnly: true, title: '志愿者注册' } },
      ],
    },
    {
      path: '/app',
      component: () => import('@/layouts/WorkspaceLayout.vue'),
      meta: { requiresAuth: true },
      redirect: '/app/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/workspace/DashboardView.vue'), meta: { title: '工作台', activeMenu: '/app/dashboard' } },
        { path: 'aid-hall', name: 'volunteer-aid-hall', component: () => import('@/views/public/AidHallView.vue'), meta: { roles: ['VOLUNTEER'], title: '需求大厅', activeMenu: '/app/aid-hall' } },
        { path: 'aid-hall/:id', name: 'volunteer-aid-detail', component: () => import('@/views/public/AidDetailView.vue'), meta: { roles: ['VOLUNTEER'], title: '需求详情', activeMenu: '/app/aid-hall' } },
        { path: 'applications', name: 'applications', component: () => import('@/views/workspace/MyApplicationsView.vue'), meta: { roles: ['VOLUNTEER'], permission: 'application:read', title: '我的申请', activeMenu: '/app/applications' } },
        { path: 'assignments', name: 'assignments', component: () => import('@/views/workspace/AssignmentsView.vue'), meta: { permission: 'assignment:read', title: '服务任务', activeMenu: '/app/assignments' } },
        { path: 'assignments/:id', name: 'assignment-detail', component: () => import('@/views/workspace/AssignmentDetailView.vue'), meta: { permission: 'assignment:read', title: '任务详情', activeMenu: '/app/assignments' } },
        { path: 'profile', name: 'profile', component: () => import('@/views/workspace/ProfileView.vue'), meta: { title: '个人中心', activeMenu: '/app/profile' } },
        { path: 'children', name: 'children', component: () => import('@/views/workspace/ChildrenView.vue'), meta: { roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'child:read', title: '儿童档案', activeMenu: '/app/children' } },
        { path: 'children/:id', name: 'child-detail', component: () => import('@/views/workspace/ChildDetailView.vue'), meta: { roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'child:read', title: '档案详情', activeMenu: '/app/children' } },
        { path: 'aid-requests', name: 'aid-requests', component: () => import('@/views/workspace/AidRequestsView.vue'), meta: { roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'aid:read', title: '帮扶需求', activeMenu: '/app/aid-requests' } },
        { path: 'aid-requests/:id', name: 'aid-request-detail', component: () => import('@/views/workspace/AidRequestDetailView.vue'), meta: { roles: ['SYS_ADMIN', 'SUPERVISOR', 'CASE_WORKER'], permission: 'aid:read', title: '需求详情', activeMenu: '/app/aid-requests' } },
        { path: 'admin/users', name: 'admin-users', component: () => import('@/views/admin/UserManagementView.vue'), meta: { roles: ['SYS_ADMIN'], permission: 'system:user:manage', title: '用户与部门', activeMenu: '/app/admin/users' } },
        { path: 'admin/roles', name: 'admin-roles', component: () => import('@/views/admin/RoleManagementView.vue'), meta: { roles: ['SYS_ADMIN'], permission: 'system:role:manage', title: '角色权限', activeMenu: '/app/admin/roles' } },
        { path: 'admin/volunteers', name: 'admin-volunteers', component: () => import('@/views/admin/VolunteerReviewView.vue'), meta: { roles: ['SYS_ADMIN'], permission: 'volunteer:review', title: '志愿者审核', activeMenu: '/app/admin/volunteers' } },
        { path: 'admin/audit', name: 'admin-audit', component: () => import('@/views/admin/AuditLogView.vue'), meta: { roles: ['SYS_ADMIN'], permission: 'system:audit:read', title: '审计日志', activeMenu: '/app/admin/audit' } },
      ],
    },
    { path: '/forbidden', name: 'forbidden', component: () => import('@/views/system/ForbiddenView.vue'), meta: { title: '无权访问' } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/system/NotFoundView.vue'), meta: { title: '页面不存在' } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.token && !auth.hydrated) {
    await auth.hydrate()
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return { path: '/app/dashboard' }
  }
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.roles?.length && (!auth.user || !to.meta.roles.includes(auth.user.roleCode))) {
    return { name: 'forbidden' }
  }
  if (to.meta.permission && !auth.hasPermission(to.meta.permission)) {
    return { name: 'forbidden' }
  }
  document.title = to.meta.title ? `${to.meta.title} | 童伴关爱` : '童伴关爱'
  return true
})

export default router
