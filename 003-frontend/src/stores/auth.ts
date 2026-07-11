import { computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import { api, TOKEN_KEY, USER_KEY } from '@/api/http'
import type { LoginResponse, MeView, RoleCode } from '@/types/models'

function readStoredUser(): MeView | null {
  const value = localStorage.getItem(USER_KEY)
  if (!value) return null
  try {
    return JSON.parse(value) as MeView
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = shallowRef(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = shallowRef<MeView | null>(readStoredUser())
  const hydrated = shallowRef(false)

  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  async function login(username: string, password: string) {
    const result = await api.post<LoginResponse>('/auth/login', { username, password })
    token.value = result.token
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  async function hydrate() {
    if (hydrated.value || !token.value) return
    try {
      user.value = await api.get<MeView>('/auth/me')
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    } catch {
      clear()
    } finally {
      hydrated.value = true
    }
  }

  async function logout() {
    try {
      await api.post<void>('/auth/logout')
    } finally {
      clear()
    }
  }

  function clear() {
    token.value = ''
    user.value = null
    hydrated.value = true
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function hasRole(...roles: RoleCode[]) {
    return user.value ? roles.includes(user.value.roleCode) : false
  }

  function hasPermission(permission: string) {
    return user.value?.permissions.includes(permission) ?? false
  }

  return { token, user, hydrated, isAuthenticated, login, hydrate, logout, clear, hasRole, hasPermission }
})
