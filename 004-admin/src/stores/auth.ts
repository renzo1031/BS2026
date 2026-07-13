import { computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import { AUTH_EXPIRED_EVENT, AUTH_REFRESHED_EVENT, request, tokenStorage } from '@/api/http'
import type { Tokens, UserRole, UserView } from '@/types/api'

const USER_KEY = 'campus-buddy-admin-user'

function restoreUser(): UserView | null {
  const value = sessionStorage.getItem(USER_KEY)
  if (!value) return null
  try {
    return JSON.parse(value) as UserView
  } catch {
    sessionStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = shallowRef<UserView | null>(restoreUser())
  const authenticated = computed(() => Boolean(user.value && tokenStorage.access()))
  const role = computed<UserRole | null>(() => user.value?.role ?? null)

  async function login(username: string, password: string) {
    const tokens = await request<Tokens>({ method: 'POST', url: '/auth/admin-login', data: { username, password } })
    tokenStorage.save(tokens)
    user.value = tokens.user
    sessionStorage.setItem(USER_KEY, JSON.stringify(tokens.user))
  }

  async function logout() {
    try {
      await request<void>({ method: 'POST', url: '/auth/logout', data: { refreshToken: tokenStorage.refresh() } })
    } finally {
      clear()
    }
  }

  function clear() {
    tokenStorage.clear()
    sessionStorage.removeItem(USER_KEY)
    user.value = null
  }

  function syncUser(event: Event) {
    const refreshedUser = (event as CustomEvent<UserView>).detail
    if (!refreshedUser) return
    user.value = refreshedUser
    sessionStorage.setItem(USER_KEY, JSON.stringify(refreshedUser))
  }

  function can(roles?: UserRole[]) {
    return !roles?.length || (role.value !== null && roles.includes(role.value))
  }

  window.addEventListener(AUTH_REFRESHED_EVENT, syncUser)
  window.addEventListener(AUTH_EXPIRED_EVENT, clear)

  return { user, authenticated, role, login, logout, clear, can }
})
