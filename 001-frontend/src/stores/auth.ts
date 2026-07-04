import { computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api/modules'
import type { User } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const token = shallowRef(localStorage.getItem('token') || '')
  const user = shallowRef<User | null>(null)
  const roles = shallowRef<string[]>(JSON.parse(localStorage.getItem('roles') || '[]'))

  const isAuthed = computed(() => Boolean(token.value))
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isStaff = computed(() => roles.value.includes('STAFF') || roles.value.includes('ADMIN'))

  function setSession(payload: { token: string; user: User; roles: string[] }) {
    token.value = payload.token
    user.value = payload.user
    roles.value = payload.roles
    localStorage.setItem('token', payload.token)
    localStorage.setItem('roles', JSON.stringify(payload.roles))
  }

  async function login(data: Record<string, unknown>) {
    setSession(await authApi.login(data))
  }

  async function register(data: Record<string, unknown>) {
    setSession(await authApi.register(data))
  }

  async function fetchMe() {
    if (!token.value) return
    const payload = await authApi.me()
    user.value = payload.user
    roles.value = payload.roles
    localStorage.setItem('roles', JSON.stringify(payload.roles))
  }

  function logout() {
    token.value = ''
    user.value = null
    roles.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('roles')
  }

  function hasAnyRole(allowed?: string[]) {
    if (!allowed || allowed.length === 0) return true
    return allowed.some((role) => roles.value.includes(role))
  }

  return { token, user, roles, isAuthed, isAdmin, isStaff, login, register, fetchMe, logout, hasAnyRole }
})
