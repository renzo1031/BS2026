import 'vue-router'
import type { RoleCode } from '@/types/models'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: RoleCode[]
    permission?: string
    activeMenu?: string
    title?: string
  }
}

export {}
