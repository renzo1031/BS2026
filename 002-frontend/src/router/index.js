import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const RoleLayout = () => import('../layouts/RoleLayout.vue')
const Login = () => import('../views/Login.vue')
const Register = () => import('../views/Register.vue')
const Portal = () => import('../views/Portal.vue')
const RequestForm = () => import('../views/RequestForm.vue')
const MyRequests = () => import('../views/MyRequests.vue')
const RequestDetail = () => import('../views/RequestDetail.vue')
const Profile = () => import('../views/Profile.vue')
const Notices = () => import('../views/Notices.vue')
const AdminStats = () => import('../views/AdminStats.vue')
const AdminRequests = () => import('../views/AdminRequests.vue')
const AdminUsers = () => import('../views/AdminUsers.vue')
const AdminCatalog = () => import('../views/AdminCatalog.vue')
const AdminLogs = () => import('../views/AdminLogs.vue')
const Forbidden = () => import('../views/Forbidden.vue')
const NotFound = () => import('../views/NotFound.vue')

const routes = [
  { path: '/login', name: 'login', component: Login, meta: { public: true } },
  { path: '/register', name: 'register', component: Register, meta: { public: true } },
  { path: '/forbidden', name: 'forbidden', component: Forbidden },
  {
    path: '/',
    component: RoleLayout,
    children: [
      { path: '', name: 'home', component: Portal, meta: { roles: ['STUDENT'] } },
      { path: 'apply', component: RequestForm, meta: { roles: ['STUDENT'] } },
      { path: 'my-requests', component: MyRequests, meta: { roles: ['STUDENT'] } },
      { path: 'requests/:id', component: RequestDetail, meta: { roles: ['STUDENT', 'STAFF', 'ADMIN'] } },
      { path: 'profile', component: Profile, meta: { roles: ['STUDENT', 'STAFF', 'ADMIN'] } },
      { path: 'notices', component: Notices, meta: { roles: ['STUDENT', 'STAFF', 'ADMIN'] } },
      { path: 'admin/stats', component: AdminStats, meta: { roles: ['ADMIN', 'STAFF'] } },
      { path: 'admin/requests', component: AdminRequests, meta: { roles: ['ADMIN', 'STAFF'] } },
      { path: 'admin/users', component: AdminUsers, meta: { roles: ['ADMIN'] } },
      { path: 'admin/catalog', component: AdminCatalog, meta: { roles: ['ADMIN'] } },
      { path: 'admin/logs', component: AdminLogs, meta: { roles: ['ADMIN'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFound, meta: { public: true } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.token && ['login', 'register'].includes(to.name)) {
      if (!auth.sessionLoaded) {
        try {
          await auth.loadMe()
        } catch {
          if (!auth.token) return true
        }
      }
      if (auth.token) return auth.homePath
    }
    return true
  }

  if (!auth.token) return { name: 'login', query: { redirect: to.fullPath } }

  if (!auth.sessionLoaded) {
    try {
      await auth.loadMe()
    } catch (error) {
      if (error.status === 401 || !auth.token) return { name: 'login', query: { redirect: to.fullPath } }
    }
  }

  if (to.name === 'home' && !auth.isStudent) return auth.homePath
  const roles = to.meta.roles
  if (roles && !roles.some((role) => auth.roles.includes(role))) return { name: 'forbidden' }
  return true
})

export default router
