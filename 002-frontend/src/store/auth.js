import { defineStore } from 'pinia'
import http from '../api/http'

function cachedUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: cachedUser(),
    sessionLoaded: false
  }),
  getters: {
    roles: (state) => state.user?.roles || [],
    isAdmin: (state) => state.user?.roles?.includes('ADMIN'),
    isStaff: (state) => state.user?.roles?.includes('STAFF'),
    isStudent: (state) => state.user?.roles?.includes('STUDENT'),
    homePath() {
      if (this.isAdmin) return '/admin/stats'
      if (this.isStaff) return '/admin/requests'
      return '/'
    }
  },
  actions: {
    persistSession() {
      if (this.token) localStorage.setItem('token', this.token)
      if (this.user) localStorage.setItem('user', JSON.stringify(this.user))
    },
    clearSession() {
      this.token = ''
      this.user = null
      this.sessionLoaded = true
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    async login(payload) {
      const result = await http.post('/auth/login', payload)
      this.token = result.token
      this.user = result.user
      this.sessionLoaded = true
      this.persistSession()
    },
    async register(payload) {
      await http.post('/auth/register', payload)
    },
    async loadMe() {
      if (!this.token) {
        this.sessionLoaded = true
        return null
      }
      try {
        this.user = await http.get('/auth/me')
        this.persistSession()
        return this.user
      } finally {
        this.sessionLoaded = true
      }
    },
    async logout() {
      try {
        if (this.token) await http.post('/auth/logout')
      } finally {
        this.clearSession()
      }
    }
  }
})
