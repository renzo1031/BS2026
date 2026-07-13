import type { Tokens, UserView } from '../types/api'

const ACCESS_KEY = 'campusBuddy.accessToken'
const REFRESH_KEY = 'campusBuddy.refreshToken'
const USER_KEY = 'campusBuddy.user'

class SessionManager {
  accessToken = ''
  refreshToken = ''
  user: UserView | null = null

  restore() {
    this.accessToken = wx.getStorageSync<string>(ACCESS_KEY) || ''
    this.refreshToken = wx.getStorageSync<string>(REFRESH_KEY) || ''
    this.user = wx.getStorageSync<UserView>(USER_KEY) || null
  }

  save(tokens: Tokens) {
    this.accessToken = tokens.accessToken
    this.refreshToken = tokens.refreshToken
    this.user = tokens.user
    wx.setStorageSync(ACCESS_KEY, tokens.accessToken)
    wx.setStorageSync(REFRESH_KEY, tokens.refreshToken)
    wx.setStorageSync(USER_KEY, tokens.user)
  }

  updateUser(user: UserView) {
    this.user = user
    wx.setStorageSync(USER_KEY, user)
  }

  clear() {
    this.accessToken = ''
    this.refreshToken = ''
    this.user = null
    wx.removeStorageSync(ACCESS_KEY)
    wx.removeStorageSync(REFRESH_KEY)
    wx.removeStorageSync(USER_KEY)
  }

  get authenticated() {
    return Boolean(this.accessToken && this.user)
  }
}

export const session = new SessionManager()
